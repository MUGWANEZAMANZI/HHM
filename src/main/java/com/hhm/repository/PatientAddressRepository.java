package com.hhm.repository;

import com.hhm.model.PatientAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientAddressRepository extends JpaRepository<PatientAddress, Long> {

    // Derived queries
    Optional<PatientAddress> findByPatientId(long patientId);
    List<PatientAddress> findByDistrict(String district);
    List<PatientAddress> findBySector(String sector);

    void deleteByPatientId(long patientId);
}
