package com.clinic.repository;

import com.clinic.domain.Patient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByNationalId(String nationalId);

    boolean existsByNationalId(String nationalId);

    List<Patient> findTop100ByOrderByCreatedAtDesc();

    @Query("""
            select p from Patient p
            where lower(p.firstName) like lower(concat('%', :q, '%'))
               or lower(p.lastName) like lower(concat('%', :q, '%'))
               or p.nationalId like concat('%', :q, '%')
            order by p.lastName, p.firstName
            """)
    List<Patient> search(@Param("q") String q);
}
