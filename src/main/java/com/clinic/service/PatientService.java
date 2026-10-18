package com.clinic.service;

import com.clinic.domain.Patient;
import com.clinic.exception.ClinicException;
import com.clinic.repository.PatientRepository;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PatientService {
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");
    private static final Pattern NATIONAL_ID = Pattern.compile("^[A-Za-z0-9-]{5,32}$");

    private final PatientRepository patients;

    @Transactional
    public Patient register(String nationalId, String firstName, String lastName, String phone, String address) {
        String nid = clean(nationalId);
        if (!NATIONAL_ID.matcher(nid).matches())
            throw new ClinicException("VALIDATION", "National ID must be 5-32 letters, digits or dashes.");
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName))
            throw new ClinicException("VALIDATION", "First and last name are required.");
        if (!PHONE.matcher(clean(phone)).matches())
            throw new ClinicException("VALIDATION", "Phone number format is invalid.");
        if (!StringUtils.hasText(address))
            throw new ClinicException("VALIDATION", "Address is required.");
        if (patients.existsByNationalId(nid))
            throw new ClinicException("DUPLICATE_PATIENT", "A patient with this National ID is already registered.");

        Patient p = new Patient();
        p.setNationalId(nid);
        p.setFirstName(firstName.trim());
        p.setLastName(lastName.trim());
        p.setPhone(clean(phone));
        p.setAddress(address.trim());
        return patients.save(p);
    }

    @Transactional(readOnly = true)
    public List<Patient> search(String q) {
        return StringUtils.hasText(q) ? patients.search(q.trim()) : patients.findTop100ByOrderByCreatedAtDesc();
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
