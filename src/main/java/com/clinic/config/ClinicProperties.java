package com.clinic.config;

import java.time.LocalTime;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic")
public record ClinicProperties(Slot slot, Queue queue, Seed seed) {
    public record Slot(List<Integer> allowedDurations, int defaultDuration, LocalTime openTime, LocalTime closeTime) {}
    public record Queue(int defaultVisitMinutes, int delayAlertMinutes) {}
    public record Seed(boolean enabled, String password) {}
}
