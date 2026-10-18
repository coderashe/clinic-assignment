package com.clinic.repository;

import com.clinic.domain.Clinician;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicianRepository extends JpaRepository<Clinician, Long> {
    Optional<Clinician> findByUserUsername(String username);

    /** Per-clinician mutex: serialises booking, check-in, call-next and queue recalculation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Clinician c where c.id = :id")
    Optional<Clinician> findByIdForUpdate(@Param("id") Long id);
}
