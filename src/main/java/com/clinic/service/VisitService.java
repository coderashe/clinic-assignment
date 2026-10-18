package com.clinic.service;

import com.clinic.domain.*;
import com.clinic.exception.ClinicException;
import com.clinic.repository.VisitRecordRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VisitService {
    private static final int MAX_TEXT = 5000;

    private final VisitRecordRepository visits;
    private final QueueService queueService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public VisitRecord get(Long queueEntryId, String username) {
        VisitRecord v = visits.findByQueueEntryId(queueEntryId)
                .orElseThrow(() -> new ClinicException("VISIT_NOT_FOUND", "No visit exists for that queue entry."));
        assertOwner(v, username);
        return v;
    }

    @Transactional
    public VisitRecord save(Long queueEntryId, String username, String diagnosis, String prescription) {
        VisitRecord v = get(queueEntryId, username);
        if (diagnosis != null && diagnosis.length() > MAX_TEXT || prescription != null && prescription.length() > MAX_TEXT)
            throw new ClinicException("VALIDATION", "Notes are limited to " + MAX_TEXT + " characters per field.");
        v.setDiagnosisNotes(trim(diagnosis));
        v.setPrescriptionSummary(trim(prescription));
        return v;
    }

    @Transactional
    public VisitRecord complete(Long queueEntryId, String username, String diagnosis, String prescription) {
        VisitRecord v = save(queueEntryId, username, diagnosis, prescription);
        if (v.getEndTime() != null)
            throw new ClinicException("ALREADY_COMPLETED", "This visit has already been completed.");
        if (!StringUtils.hasText(v.getDiagnosisNotes()))
            throw new ClinicException("VALIDATION", "Record the consultation outcome/notes before completing the visit.");

        v.setEndTime(LocalDateTime.now(clock));
        QueueEntry q = v.getQueueEntry();
        q.setQueueStatus(QueueStatus.COMPLETED);
        q.setPosition(0);
        q.setEstimatedWaitMinutes(0);
        q.getAppointment().setStatus(AppointmentStatus.COMPLETED);
        queueService.recalculateClinician(v.getClinician().getId());
        return v;
    }

    @Transactional(readOnly = true)
    public List<VisitRecord> completedToday(Long clinicianId) {
        LocalDate today = LocalDate.now(clock);
        return visits.findByClinicianIdAndEndTimeBetweenOrderByEndTimeDesc(
                clinicianId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    private void assertOwner(VisitRecord v, String username) {
        if (!v.getClinician().getUser().getUsername().equals(username))
            throw new ClinicException("FORBIDDEN", "This visit belongs to another clinician.");
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
