package com.clinic.service;

import com.clinic.config.ClinicProperties;
import com.clinic.domain.*;
import com.clinic.exception.ClinicException;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.ClinicianRepository;
import com.clinic.repository.QueueEntryRepository;
import com.clinic.repository.VisitRecordRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    /** Service order: priority tier first (EMERGENCY > URGENT > NORMAL), then first come first served. */
    public static final Comparator<QueueEntry> SERVICE_ORDER =
            Comparator.comparing(QueueEntry::getPriorityLevel).thenComparing(QueueEntry::getCheckInTime);

    private final QueueEntryRepository queue;
    private final AppointmentRepository appointments;
    private final ClinicianRepository clinicians;
    private final VisitRecordRepository visits;
    private final ClinicProperties props;
    private final Clock clock;
    private final NotificationDispatcher notifier;
    private final TransactionTemplate tx;

    /** Guards against two recalculation sweeps overlapping (e.g. a slow sweep + the next tick). */
    private final ReentrantLock sweepLock = new ReentrantLock();

    // ------------------------------------------------------------------ commands

    @Transactional
    public QueueEntry checkIn(Long appointmentId, Priority priority) {
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(() -> new ClinicException("APPOINTMENT_NOT_FOUND", "Appointment not found."));
        Long clinicianId = a.getClinician().getId();
        lockClinician(clinicianId);

        if (a.getStatus() != AppointmentStatus.BOOKED)
            throw new ClinicException("INVALID_STATE", "Only BOOKED appointments can be checked in.");
        if (!a.getAppointmentTimestamp().toLocalDate().equals(LocalDate.now(clock)))
            throw new ClinicException("INVALID_STATE", "Patients can only be checked in on the day of the appointment.");
        if (queue.findByAppointmentId(appointmentId).isPresent())
            throw new ClinicException("ALREADY_QUEUED", "This appointment is already in the queue.");

        QueueEntry q = new QueueEntry();
        q.setAppointment(a);
        q.setCheckInTime(LocalDateTime.now(clock));
        q.setPriorityLevel(priority == null ? Priority.NORMAL : priority);
        q.setQueueStatus(QueueStatus.WAITING);
        a.setStatus(AppointmentStatus.CHECKED_IN);
        queue.save(q);

        recalculateClinician(clinicianId);
        return q;
    }

    @Transactional
    public QueueEntry callNext(String username) {
        Clinician c = clinicianFor(username);
        lockClinician(c.getId());
        if (queue.existsByAppointmentClinicianIdAndQueueStatus(c.getId(), QueueStatus.WITH_CLINICIAN))
            throw new ClinicException("VISIT_IN_PROGRESS", "Complete the current visit before calling the next patient.");

        QueueEntry next = queue.findByAppointmentClinicianIdAndQueueStatus(c.getId(), QueueStatus.WAITING).stream()
                .min(SERVICE_ORDER)
                .orElseThrow(() -> new ClinicException("QUEUE_EMPTY", "No patients are waiting."));
        next.setQueueStatus(QueueStatus.WITH_CLINICIAN);
        next.setPosition(0);
        next.setEstimatedWaitMinutes(0);
        next.getAppointment().setStatus(AppointmentStatus.IN_PROGRESS);

        VisitRecord v = new VisitRecord();
        v.setQueueEntry(next);
        v.setClinician(c);
        v.setStartTime(LocalDateTime.now(clock));
        visits.save(v);

        recalculateClinician(c.getId());
        return next;
    }

    // ------------------------------------------------------------------ queries

    public void autoCheckInTodayBookings() {
        LocalDate today = LocalDate.now(clock);
        List<Appointment> todayBooked = appointments.findByStatusAndAppointmentTimestampBetween(
                AppointmentStatus.BOOKED, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        for (Appointment a : todayBooked) {
            try {
                tx.executeWithoutResult(s -> {
                    if (queue.findByAppointmentId(a.getId()).isEmpty()) {
                        checkIn(a.getId(), Priority.NORMAL);
                    }
                });
            } catch (Exception e) {
                log.warn("Auto check-in failed for appointment {}: {}", a.getId(), e.getMessage());
            }
        }
    }

    public List<QueueEntry> liveQueue() {
        autoCheckInTodayBookings();
        return queue.findByQueueStatusIn(List.of(QueueStatus.WAITING, QueueStatus.WITH_CLINICIAN)).stream()
                .sorted(Comparator.<QueueEntry, Long>comparing(q -> q.getAppointment().getClinician().getId())
                        .thenComparing(QueueEntry::getQueueStatus)
                        .thenComparing(SERVICE_ORDER))
                .toList();
    }

    public List<QueueEntry> clinicianQueue(String username) {
        autoCheckInTodayBookings();
        Clinician c = clinicianFor(username);
        return queue.findByAppointmentClinicianIdAndQueueStatusIn(
                        c.getId(), List.of(QueueStatus.WAITING, QueueStatus.WITH_CLINICIAN)).stream()
                .sorted(SERVICE_ORDER)
                .toList();
    }

    public Clinician clinicianFor(String username) {
        return clinicians.findByUserUsername(username)
                .orElseThrow(() -> new ClinicException("NOT_A_CLINICIAN", "Your account is not linked to a clinician profile."));
    }

    // ------------------------------------------------------------------ waiting-time engine

    /**
     * EWT = (waiting patients ahead in the same priority tier) x (clinician's average historical visit length).
     * Must be called inside a transaction.
     */
    public void recalculateClinician(Long clinicianId) {
        lockClinician(clinicianId);
        List<QueueEntry> waiting = queue.findByAppointmentClinicianIdAndQueueStatus(clinicianId, QueueStatus.WAITING)
                .stream().sorted(SERVICE_ORDER).toList();
        double avgMinutes = averageVisitMinutes(clinicianId);

        Map<Priority, Integer> seenPerTier = new EnumMap<>(Priority.class);
        int position = 1;
        for (QueueEntry q : waiting) {
            int ahead = seenPerTier.merge(q.getPriorityLevel(), 1, Integer::sum) - 1;
            q.setPosition(position++);
            q.setEstimatedWaitMinutes((int) Math.round(ahead * avgMinutes));
            if (!q.isDelayAlertSent() && q.getEstimatedWaitMinutes() >= props.queue().delayAlertMinutes()) {
                q.setDelayAlertSent(true);
                notifier.queueDelay(q);
            }
        }
    }

    private double averageVisitMinutes(Long clinicianId) {
        return visits.findByClinicianIdAndEndTimeIsNotNull(clinicianId).stream()
                .mapToDouble(v -> Duration.between(v.getStartTime(), v.getEndTime()).toSeconds() / 60.0)
                .average()
                .orElse(props.queue().defaultVisitMinutes());
    }

    /** Every 60s: one virtual thread per active clinician queue, each in its own transaction. */
    @Scheduled(fixedRateString = "${clinic.queue.recalc-interval-ms:60000}",
               initialDelayString = "${clinic.queue.recalc-interval-ms:60000}")
    public void recalculateAll() {
        if (!sweepLock.tryLock()) {
            log.debug("Previous queue recalculation still running; skipping this tick");
            return;
        }
        try {
            List<Long> clinicianIds = tx.execute(s -> queue.clinicianIdsWithStatus(QueueStatus.WAITING));
            if (clinicianIds == null || clinicianIds.isEmpty()) return;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = clinicianIds.stream()
                        .map(id -> executor.submit(() -> tx.executeWithoutResult(s -> recalculateClinician(id))))
                        .collect(java.util.stream.Collectors.toList());
                for (Future<?> f : futures) {
                    try {
                        f.get();
                    } catch (Exception e) {
                        log.warn("Queue recalculation failed for one clinician: {}", e.toString());
                    }
                }
            }
            log.debug("Recalculated {} clinician queue(s)", clinicianIds.size());
        } finally {
            sweepLock.unlock();
        }
    }

    private void lockClinician(Long id) {
        clinicians.findByIdForUpdate(id)
                .orElseThrow(() -> new ClinicException("CLINICIAN_NOT_FOUND", "Clinician not found."));
    }
}
