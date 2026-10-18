package com.clinic.service;

import com.clinic.config.ClinicProperties;
import com.clinic.domain.*;
import com.clinic.exception.AppointmentConflictException;
import com.clinic.exception.ClinicException;
import com.clinic.exception.InvalidSlotException;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.ClinicianRepository;
import com.clinic.repository.PatientRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final long NO_EXCLUSION = -1L;

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final ClinicianRepository clinicians;
    private final ClinicProperties props;
    private final Clock clock;
    private final NotificationDispatcher notifier;

    @Transactional
    public Appointment book(String nationalId, Long clinicianId, LocalDateTime requestedStart, Integer minutes) {
        Patient patient = patients.findByNationalId(nationalId == null ? "" : nationalId.trim())
                .orElseThrow(() -> new ClinicException("PATIENT_NOT_FOUND", "No patient is registered with that National ID."));
        // Lock the clinician row first: concurrent bookings for the same clinician queue up here,
        // so the overlap check below cannot race (it also covers the "phantom row" gap).
        Clinician clinician = lockClinician(clinicianId);

        int duration = minutes == null ? props.slot().defaultDuration() : minutes;
        LocalDateTime start = validateSlot(requestedStart, duration);
        ensureNoConflict(clinician.getId(), start, start.plusMinutes(duration), NO_EXCLUSION);

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setClinician(clinician);
        a.schedule(start, duration);
        a.setStatus(AppointmentStatus.BOOKED);
        appointments.save(a);
        notifier.appointmentConfirmed(a);
        return a;
    }

    @Transactional
    public Appointment reschedule(Long id, LocalDateTime requestedStart, Integer minutes) {
        Appointment a = find(id);
        if (a.getStatus() != AppointmentStatus.BOOKED)
            throw new ClinicException("INVALID_STATE", "Only BOOKED appointments can be rescheduled.");
        lockClinician(a.getClinician().getId());

        int duration = minutes == null ? a.getDurationMinutes() : minutes;
        LocalDateTime start = validateSlot(requestedStart, duration);
        ensureNoConflict(a.getClinician().getId(), start, start.plusMinutes(duration), a.getId());

        a.schedule(start, duration);
        notifier.appointmentConfirmed(a);
        return a;
    }

    @Transactional
    public void cancel(Long id) {
        Appointment a = find(id);
        if (a.getStatus() != AppointmentStatus.BOOKED)
            throw new ClinicException("INVALID_STATE", "Only BOOKED appointments can be cancelled.");
        a.setStatus(AppointmentStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public List<Appointment> search(String patientName, String nationalId, LocalDate from, LocalDate to,
                                    Long clinicianId, AppointmentStatus status) {
        Specification<Appointment> spec = (root, query, cb) -> {
            var patient = root.join("patient");
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(patientName)) {
                String like = "%" + patientName.trim().toLowerCase() + "%";
                var full = cb.lower(cb.concat(cb.concat(patient.<String>get("firstName"), " "), patient.<String>get("lastName")));
                ps.add(cb.like(full, like));
            }
            if (StringUtils.hasText(nationalId))
                ps.add(cb.like(patient.get("nationalId"), "%" + nationalId.trim() + "%"));
            if (from != null)
                ps.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("appointmentTimestamp"), from.atStartOfDay()));
            if (to != null)
                ps.add(cb.lessThan(root.<LocalDateTime>get("appointmentTimestamp"), to.plusDays(1).atStartOfDay()));
            if (clinicianId != null)
                ps.add(cb.equal(root.get("clinician").get("id"), clinicianId));
            if (status != null)
                ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(Predicate[]::new));
        };
        return appointments.findAll(spec, PageRequest.of(0, 200, Sort.by("appointmentTimestamp"))).getContent();
    }

    @Transactional(readOnly = true)
    public List<Appointment> bookedToday(Long clinicianId) {
        LocalDate today = LocalDate.now(clock);
        return appointments.findByClinicianIdAndStatusAndAppointmentTimestampBetweenOrderByAppointmentTimestamp(
                clinicianId, AppointmentStatus.BOOKED, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    // ------------------------------------------------------------------ validation

    /** Returns the start truncated to the minute once every slot rule has passed. */
    LocalDateTime validateSlot(LocalDateTime requested, int duration) {
        if (requested == null) throw new InvalidSlotException("Appointment date and time are required.");
        LocalDateTime start = requested.truncatedTo(ChronoUnit.MINUTES);
        var slot = props.slot();

        if (!slot.allowedDurations().contains(duration))
            throw new InvalidSlotException("Duration must be one of " + slot.allowedDurations() + " minutes.");
        if (!start.isAfter(LocalDateTime.now(clock)))
            throw new InvalidSlotException("Appointments cannot be booked or moved to a time in the past.");
        if ((start.getHour() * 60 + start.getMinute()) % duration != 0)
            throw new InvalidSlotException("Start time must fall on a " + duration + "-minute slot boundary.");

        LocalDateTime end = start.plusMinutes(duration);
        boolean withinHours = end.toLocalDate().equals(start.toLocalDate())
                && !start.toLocalTime().isBefore(slot.openTime())
                && !end.toLocalTime().isAfter(slot.closeTime());
        if (!withinHours)
            throw new InvalidSlotException("Appointments must fall within clinic hours (%s - %s)."
                    .formatted(slot.openTime(), slot.closeTime()));
        return start;
    }

    private void ensureNoConflict(Long clinicianId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Appointment> clashes = appointments.findOverlapping(
                clinicianId, AppointmentStatus.SLOT_BLOCKING, start, end, excludeId);
        if (!clashes.isEmpty()) {
            Appointment c = clashes.getFirst();
            throw new AppointmentConflictException("The clinician already has an appointment from %s to %s."
                    .formatted(c.getAppointmentTimestamp().format(FMT), c.getEndTimestamp().format(FMT)));
        }
    }

    private Clinician lockClinician(Long id) {
        return clinicians.findByIdForUpdate(id)
                .orElseThrow(() -> new ClinicException("CLINICIAN_NOT_FOUND", "Selected clinician does not exist."));
    }

    private Appointment find(Long id) {
        return appointments.findById(id)
                .orElseThrow(() -> new ClinicException("APPOINTMENT_NOT_FOUND", "Appointment not found."));
    }
}
