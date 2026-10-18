package com.clinic.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "queue_entries")
@Getter @Setter @NoArgsConstructor
public class QueueEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** unique => an appointment can never be in the queue twice. */
    @OneToOne(optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 20)
    private Priority priorityLevel = Priority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = false, length = 20)
    private QueueStatus queueStatus = QueueStatus.WAITING;

    @Column(name = "estimated_wait_minutes", nullable = false)
    private int estimatedWaitMinutes;

    @Column(name = "`position`", nullable = false)
    private int position;

    @Column(name = "delay_alert_sent", nullable = false)
    private boolean delayAlertSent;

    @Version
    private Long version;
}
