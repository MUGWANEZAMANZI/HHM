package com.service;

import com.dao.PatientAddressDAO;
import com.dao.PatientDAO;
import com.model.Patient;
import com.model.PatientAddress;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

    private final PatientDAO patientDAO;
    private final PatientAddressDAO addressDAO;

    public PatientService(PatientDAO patientDAO, PatientAddressDAO addressDAO) {
        this.patientDAO = patientDAO;
        this.addressDAO = addressDAO;
    }

    public long register(String name, String phone, String email, String address,
                         String district, String sector, String cell, String village)
            throws ServiceException {

        boolean emailTaken = patientDAO.findAll().stream()
            .anyMatch(p -> email != null && email.equalsIgnoreCase(p.getEmail()));
        if (emailTaken)
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "A patient with email \"" + email + "\" already exists.");

        Patient p = new Patient(name, phone, email);
        p.setAddress(address);
        long newId = patientDAO.insertAndGetId(p);

        if (district != null && !district.isBlank())
            addressDAO.insert(new PatientAddress(newId, district, sector, cell, village));

        return newId;
    }

    public void update(Patient patient, String district, String sector,
                       String cell, String village) throws ServiceException {
        int rows = patientDAO.update(patient);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Patient with id " + patient.getId() + " not found.");
        if (district != null && !district.isBlank()) {
            PatientAddress addr = new PatientAddress(
                patient.getId(), district, sector, cell, village);
            PatientAddress existing = addressDAO.findByPatient(patient.getId());
            if (existing == null) addressDAO.insert(addr);
            else                  addressDAO.update(addr);
        }
    }

    public void remove(long patientId) throws ServiceException {
        addressDAO.deleteByPatient(patientId);
        int rows = patientDAO.delete(patientId);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Patient with id " + patientId + " not found.");
    }

    public List<Patient> getAll() {
        return patientDAO.findAll();
    }

    public List<Patient> search(String keyword) {
        return patientDAO.search(keyword);
    }
}
