package com.clinic.service;

import com.clinic.domain.Appointment;
import com.clinic.domain.QueueEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockEmailNotificationService implements NotificationService {

    @Async
    @Override
    public void sendAppointmentConfirmation(Appointment a) {
        String recipient = a.getPatient().getEmail() != null
                ? a.getPatient().getEmail()
                : ("patient-" + a.getPatient().getId() + "@clinic.invalid");
        log.info("[EMAIL -> {}] Subject: Appointment confirmed | {} on {} with {}, room {}. thread={}",
                recipient, a.getPatient().getFullName(), a.getAppointmentTimestamp(),
                a.getClinician().displayName(), a.getClinician().getRoomNumber(), Thread.currentThread());
    }

    @Async
    @Override
    public void sendQueueDelayAlert(QueueEntry q) {
        String recipient = q.getAppointment().getPatient().getEmail() != null
                ? q.getAppointment().getPatient().getEmail()
                : ("patient-" + q.getAppointment().getPatient().getId() + "@clinic.invalid");
        log.info("[EMAIL -> {}] Subject: Queue delay | position {}, est. wait {} min. thread={}",
                recipient, q.getPosition(), q.getEstimatedWaitMinutes(),
                Thread.currentThread());
    }
}
