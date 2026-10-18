package com.clinic.web;

import com.clinic.config.ClinicProperties;
import com.clinic.domain.AppointmentStatus;
import com.clinic.domain.Priority;
import com.clinic.repository.ClinicianRepository;
import com.clinic.service.AppointmentService;
import com.clinic.service.PatientService;
import com.clinic.service.QueueService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    String patients(@RequestParam(required = false) String q, Model m) {
        m.addAttribute("patients", patientService.search(q));
        m.addAttribute("q", q);
        return "reception/patients";
    }

    @PostMapping("/patients")
    String register(@RequestParam String nationalId, @RequestParam String firstName, @RequestParam String lastName,
                    @RequestParam String phone, @RequestParam String address, RedirectAttributes ra) {
        patientService.register(nationalId, firstName, lastName, phone, address);
        ra.addFlashAttribute("success", "Patient registered.");
        return "redirect:/reception/patients";
    }

    // ---- appointments
    @GetMapping("/appointments")
    String appointments(@RequestParam(required = false) String name,
                        @RequestParam(required = false) String nationalId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(required = false) Long clinicianId,
                        @RequestParam(required = false) AppointmentStatus status,
                        Model m) {
        if (from == null && to == null) from = LocalDate.now(clock);
        m.addAttribute("appointments", appointmentService.search(name, nationalId, from, to, clinicianId, status));
        m.addAttribute("name", name);
        m.addAttribute("nationalId", nationalId);
        m.addAttribute("from", from);
        m.addAttribute("to", to);
        m.addAttribute("clinicianId", clinicianId);
        m.addAttribute("status", status);
        m.addAttribute("clinicians", clinicians.findAll());
        m.addAttribute("statuses", AppointmentStatus.values());
        m.addAttribute("priorities", Priority.values());
        m.addAttribute("durations", props.slot().allowedDurations());
        m.addAttribute("defaultDuration", props.slot().defaultDuration());
        return "reception/appointments";
    }

    @PostMapping("/appointments")
    String book(@RequestParam String nationalId, @RequestParam Long clinicianId,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                @RequestParam(required = false) Integer duration, RedirectAttributes ra) {
        appointmentService.book(nationalId, clinicianId, start, duration);
        ra.addFlashAttribute("success", "Appointment booked.");
        return "redirect:/reception/appointments";
    }

    @PostMapping("/appointments/{id}/reschedule")
    String reschedule(@PathVariable Long id,
                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                      @RequestParam(required = false) Integer duration, RedirectAttributes ra) {
        appointmentService.reschedule(id, start, duration);
        ra.addFlashAttribute("success", "Appointment rescheduled.");
        return "redirect:/reception/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    String cancel(@PathVariable Long id, RedirectAttributes ra) {
        appointmentService.cancel(id);
        ra.addFlashAttribute("success", "Appointment cancelled.");
        return "redirect:/reception/appointments";
    }

    @PostMapping("/appointments/{id}/checkin")
    String checkIn(@PathVariable Long id, @RequestParam(required = false) Priority priority, RedirectAttributes ra) {
        queueService.checkIn(id, priority);
        ra.addFlashAttribute("success", "Patient checked in to the live queue.");
        return "redirect:/reception/queue";
    }

    // ---- live queue
    @GetMapping("/queue")
    String queue(Model m) {
        m.addAttribute("entries", queueService.liveQueue());
        return "reception/queue";
    }
}
