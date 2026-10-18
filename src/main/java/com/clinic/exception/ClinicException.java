package com.clinic.exception;

public class ClinicException extends RuntimeException {
    private final String code;

    public ClinicException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
