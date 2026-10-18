package com.clinic.exception;

public class AppointmentConflictException extends ClinicException {
    public AppointmentConflictException(String message) {
        super("APPOINTMENT_CONFLICT", message);
    }
}
