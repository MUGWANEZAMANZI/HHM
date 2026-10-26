package service;

import dao.PatientDAO;
import dao.PatientFeedbackDAO;
import model.PatientFeedback;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
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
        try {
            boolean patientExists = patientDAO.findById(feedback.getPatientId()) != null;
            if (!patientExists)
                throw new ServiceException("Cannot submit feedback: patient not found.");
            feedbackDAO.insert(feedback);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public void remove(long feedbackId) throws ServiceException {
        try { feedbackDAO.delete(feedbackId); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<PatientFeedback> getAll() throws ServiceException {
        try { return feedbackDAO.findAll(); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<PatientFeedback> search(String keyword) throws ServiceException {
        try {
            String lower = keyword.toLowerCase();
            List<model.Patient> patients = patientDAO.findAll();
            return feedbackDAO.findAll().stream()
                .filter(f -> patients.stream()
                    .filter(p -> p.getId() == f.getPatientId())
                    .anyMatch(p -> p.getName().toLowerCase().contains(lower)))
                .toList();
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }
}
