package com.clinic.service;

import com.clinic.domain.Appointment;
import com.clinic.domain.QueueEntry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Fans out to every channel, but only after the surrounding transaction has committed. */
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {
    private final List<NotificationService> channels;

    public void appointmentConfirmed(Appointment a) {
        runAfterCommit(() -> channels.forEach(c -> c.sendAppointmentConfirmation(a)));
    }

    public void queueDelay(QueueEntry q) {
        runAfterCommit(() -> channels.forEach(c -> c.sendQueueDelayAlert(q)));
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
