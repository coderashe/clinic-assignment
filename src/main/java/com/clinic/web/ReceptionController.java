package com.clinic.web;

import com.clinic.config.ClinicProperties;
import com.clinic.domain.Appointment;
import com.clinic.domain.AppointmentStatus;
import com.clinic.domain.Priority;
import com.clinic.exception.AppointmentConflictException;
import com.clinic.exception.ClinicException;
import com.clinic.exception.InvalidSlotException;
import com.clinic.repository.ClinicianRepository;
import com.clinic.service.AppointmentService;
import com.clinic.service.PatientService;
import com.clinic.service.QueueService;
import com.clinic.web.dto.AppointmentBookingForm;
import com.clinic.web.dto.AppointmentFilterForm;
import com.clinic.web.dto.AppointmentRescheduleForm;
import com.clinic.web.dto.PatientForm;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reception")
@RequiredArgsConstructor
public class ReceptionController {
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final QueueService queueService;
    private final ClinicianRepository clinicians;
    private final ClinicProperties props;
    private final Clock clock;

    // ---- patients
    @GetMapping("/patients")
    public String patients(@RequestParam(required = false) String q, Model m) {
        if (!m.containsAttribute("patientForm")) {
            m.addAttribute("patientForm", new PatientForm());
        }
        m.addAttribute("patients", patientService.search(q));
        m.addAttribute("q", q);
        return "reception/patients";
    }

    @PostMapping("/patients")
    public String register(@Valid @ModelAttribute("patientForm") PatientForm form,
                           BindingResult bindingResult,
                           Model m,
                           RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            m.addAttribute("patients", patientService.search(null));
            return "reception/patients";
        }

        try {
            patientService.register(
                    form.getNationalId(),
                    form.getFirstName(),
                    form.getLastName(),
                    form.getEmail(),
                    form.getPhone(),
                    form.getAddress()
            );
            ra.addFlashAttribute("success", "Patient registered successfully.");
            return "redirect:/reception/patients";
        } catch (ClinicException ex) {
            if ("DUPLICATE_PATIENT".equals(ex.getCode())) {
                bindingResult.rejectValue("nationalId", "duplicate", ex.getMessage());
            } else {
                bindingResult.reject("registrationError", ex.getMessage());
            }
            m.addAttribute("patients", patientService.search(null));
            return "reception/patients";
        }
    }

    // ---- appointments
    @GetMapping("/appointments")
    public String appointments(@ModelAttribute("filterForm") AppointmentFilterForm filterForm,
                               BindingResult filterBinding,
                               Model m) {
        if (filterForm.getFrom() == null && filterForm.getTo() == null) {
            filterForm.setFrom(LocalDate.now(clock));
        }

        if (!filterForm.isDateRangeValid()) {
            filterBinding.rejectValue("to", "invalid.range", "'To' date cannot be before 'From' date.");
        }

        populateAppointmentsModel(m, filterForm);
        return "reception/appointments";
    }

    @PostMapping("/appointments")
    public String book(@Valid @ModelAttribute("bookingForm") AppointmentBookingForm form,
                       BindingResult bindingResult,
                       @ModelAttribute("filterForm") AppointmentFilterForm filterForm,
                       Model m,
                       RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            populateAppointmentsModel(m, filterForm);
            return "reception/appointments";
        }

        try {
            Appointment a = appointmentService.book(form.getNationalId(), form.getClinicianId(), form.getStart(), form.getDuration());
            if (a.getAppointmentTimestamp().toLocalDate().equals(LocalDate.now(clock))) {
                try {
                    queueService.checkIn(a.getId(), Priority.NORMAL);
                    ra.addFlashAttribute("success", "Appointment booked and added to live queue for " + a.getPatient().getFullName() + ".");
                } catch (Exception ex) {
                    ra.addFlashAttribute("success", "Appointment booked successfully for " + a.getPatient().getFullName() + ".");
                }
            } else {
                ra.addFlashAttribute("success", "Appointment booked successfully for " + a.getPatient().getFullName() + ".");
            }
            LocalDate d = a.getAppointmentTimestamp().toLocalDate();
            return "redirect:/reception/appointments?from=" + d + "&to=" + d;
        } catch (InvalidSlotException ex) {
            bindingResult.rejectValue("start", "invalidSlot", ex.getMessage());
            populateAppointmentsModel(m, filterForm);
            return "reception/appointments";
        } catch (AppointmentConflictException ex) {
            bindingResult.rejectValue("start", "conflict", ex.getMessage());
            populateAppointmentsModel(m, filterForm);
            return "reception/appointments";
        } catch (ClinicException ex) {
            if ("PATIENT_NOT_FOUND".equals(ex.getCode())) {
                bindingResult.rejectValue("nationalId", "patientNotFound", ex.getMessage());
            } else if ("CLINICIAN_NOT_FOUND".equals(ex.getCode())) {
                bindingResult.rejectValue("clinicianId", "clinicianNotFound", ex.getMessage());
            } else {
                bindingResult.reject("bookingError", ex.getMessage());
            }
            populateAppointmentsModel(m, filterForm);
            return "reception/appointments";
        }
    }

    @PostMapping("/appointments/{id}/reschedule")
    public String reschedule(@PathVariable Long id,
                             @Valid @ModelAttribute("rescheduleForm") AppointmentRescheduleForm form,
                             BindingResult bindingResult,
                             RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().getFirst().getDefaultMessage();
            ra.addFlashAttribute("error", "[INVALID_INPUT] " + errorMsg);
            return "redirect:/reception/appointments";
        }

        try {
            appointmentService.reschedule(id, form.getStart(), form.getDuration());
            ra.addFlashAttribute("success", "Appointment rescheduled successfully.");
        } catch (ClinicException ex) {
            ra.addFlashAttribute("error", "[" + ex.getCode() + "] " + ex.getMessage());
        }
        return "redirect:/reception/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        appointmentService.cancel(id);
        ra.addFlashAttribute("success", "Appointment cancelled successfully.");
        return "redirect:/reception/appointments";
    }

    @PostMapping("/appointments/{id}/checkin")
    public String checkIn(@PathVariable Long id,
                          @RequestParam(required = false, defaultValue = "NORMAL") Priority priority,
                          RedirectAttributes ra) {
        queueService.checkIn(id, priority);
        ra.addFlashAttribute("success", "Patient checked in to the live queue.");
        return "redirect:/reception/queue";
    }

    // ---- live queue
    @GetMapping("/queue")
    public String queue(Model m) {
        m.addAttribute("entries", queueService.liveQueue());
        return "reception/queue";
    }

    private void populateAppointmentsModel(Model m, AppointmentFilterForm filterForm) {
        if (!m.containsAttribute("bookingForm")) {
            AppointmentBookingForm bf = new AppointmentBookingForm();
            bf.setDuration(props.slot().defaultDuration());
            m.addAttribute("bookingForm", bf);
        }
        if (!m.containsAttribute("rescheduleForm")) {
            m.addAttribute("rescheduleForm", new AppointmentRescheduleForm());
        }

        m.addAttribute("appointments", appointmentService.search(
                filterForm.getName(),
                filterForm.getNationalId(),
                filterForm.getFrom(),
                filterForm.getTo(),
                filterForm.getClinicianId(),
                filterForm.getStatus()
        ));
        m.addAttribute("clinicians", clinicians.findAll());
        m.addAttribute("statuses", AppointmentStatus.values());
        m.addAttribute("priorities", Priority.values());
        m.addAttribute("durations", props.slot().allowedDurations());
        m.addAttribute("defaultDuration", props.slot().defaultDuration());
        m.addAttribute("clinicOpenTime", props.slot().openTime());
        m.addAttribute("clinicCloseTime", props.slot().closeTime());
    }
}
