package com.clinic.repository;

import com.clinic.domain.VisitRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {
    Optional<VisitRecord> findByQueueEntryId(Long queueEntryId);

    List<VisitRecord> findByClinicianIdAndEndTimeIsNotNull(Long clinicianId);

    List<VisitRecord> findByClinicianIdAndEndTimeBetweenOrderByEndTimeDesc(Long clinicianId, LocalDateTime from, LocalDateTime to);

    List<VisitRecord> findByEndTimeBetween(LocalDateTime from, LocalDateTime to);

    List<VisitRecord> findByQueueEntryCheckInTimeBetween(LocalDateTime from, LocalDateTime to);
}
