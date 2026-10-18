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
        log.info("[EMAIL -> patient-{}@clinic.invalid] Subject: Appointment confirmed | {} on {} with {}, room {}. thread={}",
                a.getPatient().getId(), a.getPatient().getFullName(), a.getAppointmentTimestamp(),
                a.getClinician().displayName(), a.getClinician().getRoomNumber(), Thread.currentThread());
    }

    @Async
    @Override
    public void sendQueueDelayAlert(QueueEntry q) {
        log.info("[EMAIL -> patient-{}@clinic.invalid] Subject: Queue delay | position {}, est. wait {} min. thread={}",
                q.getAppointment().getPatient().getId(), q.getPosition(), q.getEstimatedWaitMinutes(),
                Thread.currentThread());
    }
}
