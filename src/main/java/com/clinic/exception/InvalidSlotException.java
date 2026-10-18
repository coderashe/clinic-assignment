package com.clinic.exception;

public class InvalidSlotException extends ClinicException {
    public InvalidSlotException(String message) {
        super("INVALID_SLOT", message);
    }
}
