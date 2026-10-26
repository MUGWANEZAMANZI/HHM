package com.service;

import com.dao.PatientDAO;
import com.dao.PrescriptionDAO;
import com.model.Prescription;
import org.springframework.stereotype.Service;

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
        boolean patientExists = patientDAO.findById(prescription.getPatientId()) != null;
        if (!patientExists)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Cannot issue prescription: patient with id " + prescription.getPatientId() + " not found.");
        prescriptionDAO.insert(prescription);
    }

    public void update(Prescription prescription) throws ServiceException {
        int rows = prescriptionDAO.update(prescription);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Prescription with id " + prescription.getId() + " not found.");
    }

    // Business rule: cancel sets status instead of hard delete when prescription has items
    public void cancel(long prescriptionId) throws ServiceException {
        Prescription p = prescriptionDAO.findById(prescriptionId);
        if (p == null)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Prescription with id " + prescriptionId + " not found.");
        if ("Cancelled".equals(p.getStatus()))
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "Prescription is already cancelled.");
        p.setStatus("Cancelled");
        prescriptionDAO.update(p);
    }

    public void remove(long prescriptionId) throws ServiceException {
        int rows = prescriptionDAO.delete(prescriptionId);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Prescription with id " + prescriptionId + " not found.");
    }

    public List<Prescription> getAll() throws ServiceException {
        return prescriptionDAO.findAll();
    }
}
