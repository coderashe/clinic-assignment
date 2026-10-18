package com.clinic.domain;

import java.util.List;

public enum AppointmentStatus {
    BOOKED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED;

    /** Statuses that occupy a clinician's slot. */
    public static final List<AppointmentStatus> SLOT_BLOCKING = List.of(BOOKED, CHECKED_IN, IN_PROGRESS);

    public String badge() {
        return switch (this) {
            case BOOKED -> "text-bg-primary";
            case CHECKED_IN -> "text-bg-info";
            case IN_PROGRESS -> "text-bg-warning";
            case COMPLETED -> "text-bg-success";
            case CANCELLED -> "text-bg-secondary";
        };
    }
}
