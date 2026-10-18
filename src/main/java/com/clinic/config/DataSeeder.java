package com.clinic.config;

import com.clinic.domain.*;
import com.clinic.repository.ClinicianRepository;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates demo accounts on an empty database. Disable with clinic.seed.enabled=false. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final ClinicianRepository clinicians;
    private final PatientRepository patients;
    private final PasswordEncoder encoder;
    private final ClinicProperties props;

    @Override
    @Transactional
    public void run(String... args) {
        if (!props.seed().enabled() || users.count() > 0) return;
        String hash = encoder.encode(props.seed().password());

        user("reception", "Rudo Receptionist", Role.RECEPTIONIST, hash);
        user("manager", "Mark Manager", Role.MANAGER, hash);
        clinician(user("dr.moyo", "Dr. Tendai Moyo", Role.CLINICIAN, hash), "General Practice", "Room 1");
        clinician(user("dr.ncube", "Dr. Sipho Ncube", Role.CLINICIAN, hash), "Paediatrics", "Room 2");

        patient("63-123456A12", "Tafara", "Dube", "+263 77 123 4567", "12 Samora Machel Ave, Harare");
        patient("63-654321B34", "Chipo", "Sibanda", "+263 71 765 4321", "8 Jason Moyo St, Bulawayo");

        log.warn("Seeded demo users (reception, manager, dr.moyo, dr.ncube) with the configured seed password. "
                + "Change it and set clinic.seed.enabled=false before any real use.");
    }

    private User user(String username, String fullName, Role role, String hash) {
        User u = new User();
        u.setUsername(username);
        u.setFullName(fullName);
        u.setRole(role);
        u.setPassword(hash);
        return users.save(u);
    }

    private void clinician(User u, String specialization, String room) {
        Clinician c = new Clinician();
        c.setUser(u);
        c.setSpecialization(specialization);
        c.setRoomNumber(room);
        clinicians.save(c);
    }

    private void patient(String nid, String first, String last, String phone, String address) {
        Patient p = new Patient();
        p.setNationalId(nid);
        p.setFirstName(first);
        p.setLastName(last);
        p.setPhone(phone);
        p.setAddress(address);
        patients.save(p);
    }
}
