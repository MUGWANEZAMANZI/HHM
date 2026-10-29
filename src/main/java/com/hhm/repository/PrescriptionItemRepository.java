package com.hhm.repository;

import com.hhm.model.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    // Derived queries
    List<PrescriptionItem> findByPrescriptionId(long prescriptionId);
    List<PrescriptionItem> findByMedicationId(long medicationId);
    void deleteByPrescriptionId(long prescriptionId);
}
