package com.service;

import com.model.Prescription;
import com.repository.PatientRepository;
import com.repository.PrescriptionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PatientRepository patientRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    @CacheEvict(value = "prescriptions", allEntries = true)
    public void issue(Prescription prescription) throws ServiceException {
        // Business rule: patient must exist before issuing a prescription
        if (!patientRepository.existsById(prescription.getPatientId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Cannot issue prescription: patient with id " + prescription.getPatientId() + " not found.");
        prescriptionRepository.save(prescription);
    }

    @Transactional
    @CacheEvict(value = "prescriptions", allEntries = true)
    public void update(Prescription prescription) throws ServiceException {
        if (!prescriptionRepository.existsById(prescription.getId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Prescription with id " + prescription.getId() + " not found.");
        prescriptionRepository.save(prescription);
    }

    @Transactional
    @CacheEvict(value = "prescriptions", allEntries = true)
    public void cancel(long prescriptionId) throws ServiceException {
        // Business rule: cancel sets status instead of hard delete when prescription has items
        Prescription p = prescriptionRepository.findById(prescriptionId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Prescription with id " + prescriptionId + " not found."));

        if ("Cancelled".equals(p.getStatus()))
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "Prescription is already cancelled.");

        p.setStatus("Cancelled");
        prescriptionRepository.save(p);
    }

    @Transactional
    @CacheEvict(value = "prescriptions", allEntries = true)
    public void remove(long prescriptionId) throws ServiceException {
        if (!prescriptionRepository.existsById(prescriptionId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Prescription with id " + prescriptionId + " not found.");
        prescriptionRepository.deleteById(prescriptionId);
    }

    @Transactional(readOnly = true)
    @Cacheable("prescriptions")
    public List<Prescription> getAll() {
        return prescriptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "prescriptions", key = "#patientId")
    public List<Prescription> getByPatient(long patientId) {
        return prescriptionRepository.findByPatientId(patientId);
    }
}
