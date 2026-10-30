package com.hhm.service;

import com.hhm.model.Patient;
import com.hhm.model.PatientAddress;
import com.hhm.repository.PatientAddressRepository;
import com.hhm.repository.PatientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientAddressRepository addressRepository;

    public PatientService(PatientRepository patientRepository,
                          PatientAddressRepository addressRepository) {
        this.patientRepository = patientRepository;
        this.addressRepository = addressRepository;
    }

    @Transactional
    @CacheEvict(value = "patients", allEntries = true)
    public long register(String name, String phone, String email, String address,
                         String district, String sector, String cell, String village)
            throws ServiceException {

        if (patientRepository.findByEmail(email).isPresent())
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "A patient with email \"" + email + "\" already exists.");

        if (patientRepository.findByPhone(phone).isPresent())
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "A patient with phone \"" + phone + "\" already exists.");

        Patient p = new Patient(name, phone, email);
        p.setAddress(address);
        Patient saved = patientRepository.save(p);

        if (district != null && !district.isBlank())
            addressRepository.save(new PatientAddress(saved.getId(), district, sector, cell, village));

        return saved.getId();
    }

    @Transactional
    @CacheEvict(value = "patients", allEntries = true)
    public void update(Patient patient, String district, String sector,
                       String cell, String village) throws ServiceException {

        if (!patientRepository.existsById(patient.getId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Patient with id " + patient.getId() + " not found.");

        patientRepository.findByEmail(patient.getEmail()).ifPresent(found -> {
            if (found.getId() != patient.getId())
                throw new ServiceException(ServiceException.Type.CONFLICT,
                    "Email \"" + patient.getEmail() + "\" is already used by another patient.");
        });

        patientRepository.save(patient);

        if (district != null && !district.isBlank()) {
            PatientAddress addr = new PatientAddress(patient.getId(), district, sector, cell, village);
            addressRepository.findByPatientId(patient.getId())
                             .ifPresent(existing -> addr.setId(existing.getId()));
            addressRepository.save(addr);
        }
    }

    @Transactional
    @CacheEvict(value = "patients", allEntries = true)
    public void remove(long patientId) throws ServiceException {
        if (!patientRepository.existsById(patientId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Patient with id " + patientId + " not found.");

        addressRepository.deleteByPatientId(patientId);
        patientRepository.deleteById(patientId);
    }

    @Transactional(readOnly = true)
    @Cacheable("patients")
    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Patient> getAll(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "patients", key = "#keyword")
    public List<Patient> search(String keyword) {
        return patientRepository.search(keyword);
    }
}
