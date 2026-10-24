package com.clinic.web;

import com.clinic.domain.Clinician;
import com.clinic.domain.Priority;
import com.clinic.domain.QueueEntry;
import com.clinic.domain.QueueStatus;
import com.clinic.service.AppointmentService;
import com.clinic.service.QueueService;
import com.clinic.service.VisitService;
import com.clinic.web.dto.VisitNotesForm;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clinician")
@RequiredArgsConstructor
public class ClinicianController {
    private final QueueService queueService;
    private final VisitService visitService;
    private final AppointmentService appointmentService;

    @GetMapping("/queue")
    public String queue(Principal principal, Model m) {
        Clinician c = queueService.clinicianFor(principal.getName());
        Map<Boolean, List<QueueEntry>> split = queueService.clinicianQueue(principal.getName()).stream()
                .collect(Collectors.partitioningBy(q -> q.getQueueStatus() == QueueStatus.WITH_CLINICIAN));
        m.addAttribute("clinician", c);
        m.addAttribute("current", split.get(true).stream().findFirst().orElse(null));
        m.addAttribute("waiting", split.get(false));
        m.addAttribute("booked", appointmentService.bookedToday(c.getId()));
        m.addAttribute("done", visitService.completedToday(c.getId()));
        return "clinician/queue";
    }

    @PostMapping("/queue/next")
    public String callNext(Principal principal) {
        QueueEntry next = queueService.callNext(principal.getName());
        return "redirect:/clinician/visits/" + next.getId();
    }

    @PostMapping("/appointments/{id}/checkin")
    public String checkIn(@PathVariable Long id, RedirectAttributes ra) {
        queueService.checkIn(id, Priority.NORMAL);
        ra.addFlashAttribute("success", "Patient checked in to waiting queue.");
        return "redirect:/clinician/queue";
    }

    @GetMapping("/visits/{queueEntryId}")
    public String visit(@PathVariable Long queueEntryId, Principal principal, Model m) {
        var visit = visitService.get(queueEntryId, principal.getName());
        if (!m.containsAttribute("visitNotesForm")) {
            VisitNotesForm form = new VisitNotesForm();
            form.setDiagnosis(visit.getDiagnosisNotes());
            form.setPrescription(visit.getPrescriptionSummary());
            m.addAttribute("visitNotesForm", form);
        }
        m.addAttribute("visit", visit);
        return "clinician/visit";
    }

    @PostMapping("/visits/{queueEntryId}/save")
    public String save(@PathVariable Long queueEntryId,
                       @Valid @ModelAttribute("visitNotesForm") VisitNotesForm form,
                       BindingResult bindingResult,
                       Principal principal,
                       Model m,
                       RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            m.addAttribute("visit", visitService.get(queueEntryId, principal.getName()));
            return "clinician/visit";
        }
        visitService.save(queueEntryId, principal.getName(), form.getDiagnosis(), form.getPrescription());
        ra.addFlashAttribute("success", "Visit notes saved.");
        return "redirect:/clinician/visits/" + queueEntryId;
    }

    @PostMapping("/visits/{queueEntryId}/complete")
    public String complete(@PathVariable Long queueEntryId,
                           @Valid @ModelAttribute("visitNotesForm") VisitNotesForm form,
                           BindingResult bindingResult,
                           Principal principal,
                           Model m,
                           RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            m.addAttribute("visit", visitService.get(queueEntryId, principal.getName()));
            return "clinician/visit";
        }
        visitService.complete(queueEntryId, principal.getName(), form.getDiagnosis(), form.getPrescription());
        ra.addFlashAttribute("success", "Visit completed.");
        return "redirect:/clinician/queue";
    }
}
