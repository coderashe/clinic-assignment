package com.clinic.web.dto;

import com.clinic.domain.AppointmentStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class AppointmentFilterForm {
    private String name;
    private String nationalId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    private Long clinicianId;
    private AppointmentStatus status;

    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
