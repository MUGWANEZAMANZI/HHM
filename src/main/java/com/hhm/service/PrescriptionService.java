package com.hhm.service;

import com.hhm.model.Doctor;
import com.hhm.model.Patient;
import com.hhm.model.Prescription;
import com.hhm.repository.DoctorRepository;
import com.hhm.repository.PatientRepository;
import com.hhm.repository.PrescriptionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PatientRepository patientRepository,
                               DoctorRepository doctorRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    @CacheEvict(value = "prescriptions", allEntries = true)
    public void issue(long patientId, long doctorId, LocalDateTime issuedAt, String status, String notes)
            throws ServiceException {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Cannot issue prescription: patient with id " + patientId + " not found."));
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Cannot issue prescription: doctor with id " + doctorId + " not found."));
        prescriptionRepository.save(new Prescription(patient, doctor, issuedAt, status, notes));
    }

    @Transactional
    @CacheEvict(value = "prescriptions", allEntries = true)
    public void update(long id, long patientId, long doctorId, LocalDateTime issuedAt,
                       String status, String notes) throws ServiceException {
        Prescription p = prescriptionRepository.findById(id)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Prescription with id " + id + " not found."));
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Patient with id " + patientId + " not found."));
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Doctor with id " + doctorId + " not found."));
        p.setPatient(patient);
        p.setDoctor(doctor);
        p.setIssuedAt(issuedAt);
        p.setStatus(status);
        p.setNotes(notes);
        prescriptionRepository.save(p);
    }

    @Transactional
    @CacheEvict(value = "prescriptions", allEntries = true)
    public void cancel(long prescriptionId) throws ServiceException {
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

}
