package com.clinic.web;

import com.clinic.domain.QueueEntry;
import com.clinic.domain.QueueStatus;
import com.clinic.domain.Clinician;
import com.clinic.service.AppointmentService;
import com.clinic.service.QueueService;
import com.clinic.service.VisitService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    String queue(Principal principal, Model m) {
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
    String callNext(Principal principal) {
        QueueEntry next = queueService.callNext(principal.getName());
        return "redirect:/clinician/visits/" + next.getId();
    }

    @GetMapping("/visits/{queueEntryId}")
    String visit(@PathVariable Long queueEntryId, Principal principal, Model m) {
        m.addAttribute("visit", visitService.get(queueEntryId, principal.getName()));
        return "clinician/visit";
    }

    @PostMapping("/visits/{queueEntryId}/save")
    String save(@PathVariable Long queueEntryId, @RequestParam(required = false) String diagnosis,
                @RequestParam(required = false) String prescription, Principal principal, RedirectAttributes ra) {
        visitService.save(queueEntryId, principal.getName(), diagnosis, prescription);
        ra.addFlashAttribute("success", "Visit notes saved.");
        return "redirect:/clinician/visits/" + queueEntryId;
    }

    @PostMapping("/visits/{queueEntryId}/complete")
    String complete(@PathVariable Long queueEntryId, @RequestParam(required = false) String diagnosis,
                    @RequestParam(required = false) String prescription, Principal principal, RedirectAttributes ra) {
        visitService.complete(queueEntryId, principal.getName(), diagnosis, prescription);
        ra.addFlashAttribute("success", "Visit completed.");
        return "redirect:/clinician/queue";
    }
}
