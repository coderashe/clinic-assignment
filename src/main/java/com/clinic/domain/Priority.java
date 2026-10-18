package com.clinic.domain;

/** Declaration order is the service order: EMERGENCY first. */
public enum Priority {
    EMERGENCY, URGENT, NORMAL;

    public String badge() {
        return switch (this) {
            case EMERGENCY -> "text-bg-danger";
            case URGENT -> "text-bg-warning";
            case NORMAL -> "text-bg-secondary";
        };
    }
}
