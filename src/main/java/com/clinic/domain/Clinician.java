package com.clinic.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clinicians")
@Getter @Setter @NoArgsConstructor
public class Clinician {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 80)
    private String specialization;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    public String displayName() {
        return user.getFullName();
    }
}
