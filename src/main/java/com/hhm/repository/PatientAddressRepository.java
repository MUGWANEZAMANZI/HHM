package com.hhm.repository;

import com.hhm.model.PatientAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientAddressRepository extends JpaRepository<PatientAddress, Long> {
    Optional<PatientAddress> findByPatient_Id(long patientId);

    void deleteByPatient_Id(long patientId);
}
