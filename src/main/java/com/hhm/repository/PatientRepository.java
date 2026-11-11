package com.hhm.repository;

import com.hhm.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Derived queries
    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByPhone(String phone);

    // JPQL — multi-field search across name, email, phone
    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "p.phone LIKE CONCAT('%', :kw, '%')")
    List<Patient> search(@Param("kw") String keyword);

}
