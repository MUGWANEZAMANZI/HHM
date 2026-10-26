package service;

import dao.PatientAddressDAO;
import dao.PatientDAO;
import model.Patient;
import model.PatientAddress;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class PatientService {

    private final PatientDAO     patientDAO;
    private final PatientAddressDAO addressDAO;

    //Constructor injection for DAOs
    public PatientService(PatientDAO patientDAO, PatientAddressDAO addressDAO)
    {
        this.patientDAO = patientDAO;
        this.addressDAO = addressDAO;
    }

    // Business rule: email must be unique across existing patients
    public long register(String name, String phone, String email, String address,
                         String district, String sector, String cell, String village)
            throws ServiceException {
        try {
            boolean emailTaken = patientDAO.findAll().stream()
                .anyMatch(p -> email != null && email.equalsIgnoreCase(p.getEmail()));
            if (emailTaken)
                throw new ServiceException("A patient with email \"" + email + "\" already exists.");

            Patient p = new Patient(name, phone, email);
            p.setAddress(address);
            long newId = patientDAO.insertAndGetId(p);

            if (district != null && !district.isBlank())
                addressDAO.insert(new PatientAddress(newId, district, sector, cell, village));

            return newId;
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public void update(Patient patient, String district, String sector,
                       String cell, String village) throws ServiceException {
        try {
            patientDAO.update(patient);
            boolean hasAddress = district != null && !district.isBlank();
            if (hasAddress) {
                PatientAddress addr = new PatientAddress(
                    patient.getId(), district, sector, cell, village);
                PatientAddress existing = addressDAO.findByPatient(patient.getId());
                if (existing == null) addressDAO.insert(addr);
                else                  addressDAO.update(addr);
            }
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    // Business rule: deletes address first to satisfy FK, then the patient
    public void remove(long patientId) throws ServiceException {
        try {
            addressDAO.deleteByPatient(patientId);
            patientDAO.delete(patientId);
        } catch (SQLException e) {
            throw new ServiceException("Cannot delete patient: " + e.getMessage(), e);
        }
    }

    public List<Patient> getAll() throws ServiceException {
        try { return patientDAO.findAll(); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<Patient> search(String keyword) throws ServiceException {
        try { return patientDAO.search(keyword); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }
}
