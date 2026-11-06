package com.hhm.service;

import com.hhm.model.Patient;
import com.hhm.model.PatientAddress;
import com.hhm.repository.PatientAddressRepository;
import com.hhm.repository.PatientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    @CacheEvict(value = "patients-search", allEntries = true)
    public long register(String name, String phone, String email, String address,
                         String district, String sector, String cell, String village)
            throws ServiceException {

        if (patientRepository.findByEmail(email).isPresent())
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "A patient with email \"" + email + "\" already exists.");

        if (patientRepository.findByPhone(phone).isPresent())
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "A patient with phone \"" + phone + "\" already exists.");

        Patient p = new Patient(name, phone);
        p.setEmail(email);
        p.setAddress(address);
        Patient saved = patientRepository.save(p);

        if (district != null && !district.isBlank())
            addressRepository.save(new PatientAddress(saved, district, sector, cell, village));

        return saved.getId();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "patient",          key  = "#patient.id"),
        @CacheEvict(value = "patients-search",  allEntries = true)
    })
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
            PatientAddress addr = new PatientAddress(patient, district, sector, cell, village);
            addressRepository.findByPatient_Id(patient.getId())
                             .ifPresent(existing -> addr.setId(existing.getId()));
            addressRepository.save(addr);
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "patient",          key  = "#patientId"),
        @CacheEvict(value = "patients-search",  allEntries = true)
    })
    public void remove(long patientId) throws ServiceException {
        if (!patientRepository.existsById(patientId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Patient with id " + patientId + " not found.");

        addressRepository.deleteByPatient_Id(patientId);
        patientRepository.deleteById(patientId);
    }

    // Page<T> is never cacheable — PageImpl has no default constructor so Jackson
    // can serialize it into Redis but cannot reconstruct it on read-back (500 error).
    // DB-level pagination with an index is fast enough without caching.
    @Transactional(readOnly = true)
    public Page<Patient> getAll(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }

    // Safe to cache: returns List<Patient> (plain POJO list), keyed by keyword.
    // Evicted on any write so stale search results are never served.
    @Transactional(readOnly = true)
    @Cacheable(value = "patients-search", key = "#keyword")
    public List<Patient> search(String keyword) {
        return patientRepository.search(keyword);
    }

    // Safe to cache: returns a single Patient keyed by ID.
    // Evicted precisely on update/delete of that patient — no allEntries needed.
    @Transactional(readOnly = true)
    @Cacheable(value = "patient", key = "#id")
    public Patient findById(long id) throws ServiceException {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                        "Patient with id " + id + " not found."));
    }
}
