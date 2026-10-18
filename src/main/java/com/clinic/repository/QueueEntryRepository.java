package com.clinic.repository;

import com.clinic.domain.QueueEntry;
import com.clinic.domain.QueueStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    Optional<QueueEntry> findByAppointmentId(Long appointmentId);

    List<QueueEntry> findByQueueStatusIn(Collection<QueueStatus> statuses);

    List<QueueEntry> findByAppointmentClinicianIdAndQueueStatus(Long clinicianId, QueueStatus status);

    List<QueueEntry> findByAppointmentClinicianIdAndQueueStatusIn(Long clinicianId, Collection<QueueStatus> statuses);

    boolean existsByAppointmentClinicianIdAndQueueStatus(Long clinicianId, QueueStatus status);

    List<QueueEntry> findByCheckInTimeBetween(LocalDateTime from, LocalDateTime to);

    @Query("select distinct q.appointment.clinician.id from QueueEntry q where q.queueStatus = :status")
    List<Long> clinicianIdsWithStatus(@Param("status") QueueStatus status);
}
