package com.hhm.repository;

import com.hhm.model.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatient_Id(long patientId);
    List<Prescription> findByDoctor_Id(long doctorId);
    List<Prescription> findByStatus(String status);
    List<Prescription> findByPatient_IdAndStatus(long patientId, String status);
    Page<Prescription> findByPatient_Id(long patientId, Pageable pageable);
}
