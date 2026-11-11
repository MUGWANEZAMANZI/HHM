package com.hhm.repository;

import com.hhm.model.PatientFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientFeedbackRepository extends JpaRepository<PatientFeedback, Long> {
    List<PatientFeedback> findByPatient_Id(long patientId);
    List<PatientFeedback> findByFeedbackContainingIgnoreCase(String keyword);
}
