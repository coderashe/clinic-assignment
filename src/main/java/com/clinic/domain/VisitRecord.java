package com.clinic.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "visit_records")
@Getter @Setter @NoArgsConstructor
public class VisitRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "queue_entry_id", nullable = false, unique = true)
    private QueueEntry queueEntry;

    @ManyToOne(optional = false)
    @JoinColumn(name = "clinician_id", nullable = false)
    private Clinician clinician;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "diagnosis_notes", columnDefinition = "TEXT")
    private String diagnosisNotes;

    @Column(name = "prescription_summary", columnDefinition = "TEXT")
    private String prescriptionSummary;

    @Version
    private Long version;
}
