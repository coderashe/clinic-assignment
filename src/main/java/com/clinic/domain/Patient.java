package com.clinic.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patients")
@Getter @Setter @NoArgsConstructor
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "national_id", nullable = false, unique = true, length = 32)
    private String nationalId;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ---- field-level protection: non-receptionist views must use these ----
    public String maskedNationalId() { return mask(nationalId, 3); }
    public String maskedPhone() { return mask(phone, 3); }
    public String maskedEmail() {
        if (email == null || !email.contains("@")) return "****";
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String domain = email.substring(at);
        if (name.length() <= 2) return "*".repeat(name.length()) + domain;
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1) + domain;
    }

    private static String mask(String s, int keep) {
        if (s == null || s.length() <= keep) return "****";
        return "*".repeat(s.length() - keep) + s.substring(s.length() - keep);
    }
}
