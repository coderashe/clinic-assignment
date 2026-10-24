package com.clinic.repository;

import com.clinic.domain.Appointment;
import com.clinic.domain.AppointmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    /** Interval overlap: existing [start,end) blocks new [S,E) when start < E and end > S. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from Appointment a
            where a.clinician.id = :clinicianId
              and a.status in :statuses
              and a.appointmentTimestamp < :end
              and a.endTimestamp > :start
              and a.id <> :excludeId
            order by a.appointmentTimestamp
            """)
    List<Appointment> findOverlapping(@Param("clinicianId") Long clinicianId,
                                      @Param("statuses") Collection<AppointmentStatus> statuses,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("excludeId") Long excludeId);

    List<Appointment> findByClinicianIdAndStatusAndAppointmentTimestampBetweenOrderByAppointmentTimestamp(
            Long clinicianId, AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByAppointmentTimestampBetween(LocalDateTime from, LocalDateTime to);

    List<Appointment> findByStatusAndAppointmentTimestampBetween(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to);
}
