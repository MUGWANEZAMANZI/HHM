package com.hhm.repository;

import com.hhm.model.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    // Derived queries
    List<Prescription> findByPatientId(long patientId);
    List<Prescription> findByDoctorId(long doctorId);
    List<Prescription> findByStatus(String status);
    List<Prescription> findByPatientIdAndStatus(long patientId, String status);

    // Pagination
    Page<Prescription> findByPatientId(long patientId, Pageable pageable);
}
