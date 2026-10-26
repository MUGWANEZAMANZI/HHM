package com.service;

import com.model.Patient;
import com.dao.PatientDAO;
import com.dao.PatientFeedbackDAO;
import com.model.PatientFeedback;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientFeedbackService {

    private final PatientFeedbackDAO feedbackDAO;
    private final PatientDAO         patientDAO;


    PatientFeedbackService(PatientFeedbackDAO feedbackDAO, PatientDAO patientDAO)
    {
        this.feedbackDAO = feedbackDAO;
        this.patientDAO = patientDAO;
    }
    // Business rule: feedback text must not be empty, patient must exist
    public void submit(PatientFeedback feedback) throws ServiceException {
        if (feedback.getFeedback() == null || feedback.getFeedback().isBlank())
            throw new ServiceException("Feedback text cannot be empty.");
        boolean patientExists = patientDAO.findById(feedback.getPatientId()) != null;
        if (!patientExists)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Cannot submit feedback: patient with id " + feedback.getPatientId() + " not found.");
        feedbackDAO.insert(feedback);
    }

    public void remove(long feedbackId) throws ServiceException {
        int rows = feedbackDAO.delete(feedbackId);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Feedback with id " + feedbackId + " not found.");
    }

    public List<PatientFeedback> getAll() throws ServiceException {
        return feedbackDAO.findAll();
    }

    public List<PatientFeedback> search(String keyword) throws ServiceException {
        String lower = keyword.toLowerCase();
        List<Patient> patients = patientDAO.findAll();
        return feedbackDAO.findAll().stream()
            .filter(f -> patients.stream()
                .filter(p -> p.getId() == f.getPatientId())
                .anyMatch(p -> p.getName().toLowerCase().contains(lower)))
            .toList();
    }
}
