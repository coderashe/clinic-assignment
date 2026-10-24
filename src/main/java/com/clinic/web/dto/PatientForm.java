package com.clinic.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientForm {

    @NotBlank(message = "National ID is required.")
    @Pattern(regexp = "^[A-Za-z0-9-]{5,32}$", message = "National ID must be 5 to 32 letters, digits, or dashes.")
    private String nationalId;

    @NotBlank(message = "First name is required.")
    @Size(max = 80, message = "First name must not exceed 80 characters.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 80, message = "Last name must not exceed 80 characters.")
    private String lastName;

    @NotBlank(message = "Email address is required.")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please provide a valid email address (e.g. user@example.com).")
    @Size(max = 120, message = "Email must not exceed 120 characters.")
    private String email;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Phone number must be between 7 and 20 digits (e.g. +263 77 123 4567).")
    private String phone;

    @NotBlank(message = "Address is required.")
    @Size(max = 255, message = "Address must not exceed 255 characters.")
    private String address;
}
