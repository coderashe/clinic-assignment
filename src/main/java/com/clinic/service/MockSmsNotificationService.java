package com.clinic.service;

import com.clinic.domain.Appointment;
import com.clinic.domain.QueueEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockSmsNotificationService implements NotificationService {

    @Async
    @Override
    public void sendAppointmentConfirmation(Appointment a) {
        log.info("[SMS -> {}] Hi {}, your appointment with {} is confirmed for {} ({} min). thread={}",
                a.getPatient().maskedPhone(), a.getPatient().getFirstName(), a.getClinician().displayName(),
                a.getAppointmentTimestamp(), a.getDurationMinutes(), Thread.currentThread());
    }

    @Async
    @Override
    public void sendQueueDelayAlert(QueueEntry q) {
        log.info("[SMS -> {}] Hi {}, current estimated wait is about {} minutes. Thank you for your patience. thread={}",
                q.getAppointment().getPatient().maskedPhone(), q.getAppointment().getPatient().getFirstName(),
                q.getEstimatedWaitMinutes(), Thread.currentThread());
    }
}
