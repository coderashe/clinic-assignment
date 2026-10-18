package com.clinic.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointments", indexes = @Index(name = "idx_appt_clinician_time",
        columnList = "clinician_id, appointment_timestamp, end_timestamp"))
@Getter @Setter @NoArgsConstructor
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "clinician_id", nullable = false)
    private Clinician clinician;

    @Column(name = "appointment_timestamp", nullable = false)
    private LocalDateTime appointmentTimestamp;

    /** Derived (start + duration) and persisted so overlap checks stay a simple indexed range query. */
    @Column(name = "end_timestamp", nullable = false)
    private LocalDateTime endTimestamp;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Version
    private Long version;

    public void schedule(LocalDateTime start, int minutes) {
        this.appointmentTimestamp = start;
        this.durationMinutes = minutes;
        this.endTimestamp = start.plusMinutes(minutes);
    }
}
