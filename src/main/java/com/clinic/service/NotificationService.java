package com.clinic.service;

import com.clinic.domain.Appointment;
import com.clinic.domain.QueueEntry;

public interface NotificationService {
    void sendAppointmentConfirmation(Appointment appointment);

    void sendQueueDelayAlert(QueueEntry entry);
}
