package service;

import dao.PatientDAO;
import dao.PrescriptionDAO;
import model.Prescription;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionDAO prescriptionDAO;
    private final PatientDAO      patientDAO;

    //Constructor injection for DAOs
    public PrescriptionService(PrescriptionDAO prescriptionDAO, PatientDAO patientDAO)
    {
        this.prescriptionDAO = prescriptionDAO;
        this.patientDAO = patientDAO;
    }

    // Business rule: patient must exist before issuing a prescription
    public void issue(Prescription prescription) throws ServiceException {
        try {
            boolean patientExists = patientDAO.findById(prescription.getPatientId()) != null;
            if (!patientExists)
                throw new ServiceException("Cannot issue prescription: patient not found.");
            prescriptionDAO.insert(prescription);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public void update(Prescription prescription) throws ServiceException {
        try { prescriptionDAO.update(prescription); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    // Business rule: cancel sets status instead of hard delete when prescription has items
    public void cancel(long prescriptionId) throws ServiceException {
        try {
            Prescription p = prescriptionDAO.findById(prescriptionId);
            if (p == null) throw new ServiceException("Prescription not found.");
            if ("Cancelled".equals(p.getStatus()))
                throw new ServiceException("Prescription is already cancelled.");
            p.setStatus("Cancelled");
            prescriptionDAO.update(p);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public void remove(long prescriptionId) throws ServiceException {
        try { prescriptionDAO.delete(prescriptionId); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<Prescription> getAll() throws ServiceException {
        try { return prescriptionDAO.findAll(); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }
}
