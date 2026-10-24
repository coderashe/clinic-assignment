package com.clinic.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VisitNotesForm {

    @Size(max = 5000, message = "Diagnosis notes must not exceed 5000 characters.")
    private String diagnosis;

    @Size(max = 5000, message = "Prescription summary must not exceed 5000 characters.")
    private String prescription;
}
