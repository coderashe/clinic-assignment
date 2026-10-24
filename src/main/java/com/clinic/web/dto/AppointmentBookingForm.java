package com.clinic.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class AppointmentBookingForm {

    @NotBlank(message = "Patient National ID is required.")
    private String nationalId;

    @NotNull(message = "Please select a clinician.")
    private Long clinicianId;

    @NotNull(message = "Appointment date and time are required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime start;

    @NotNull(message = "Please select a duration.")
    private Integer duration;
}
