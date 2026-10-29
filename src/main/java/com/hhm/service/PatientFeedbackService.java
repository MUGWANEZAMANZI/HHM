package com.service;

import com.model.PatientFeedback;
import com.repository.PatientFeedbackRepository;
import com.repository.PatientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientFeedbackService {

    private final PatientFeedbackRepository feedbackRepository;
    private final PatientRepository patientRepository;

    public PatientFeedbackService(PatientFeedbackRepository feedbackRepository,
                                  PatientRepository patientRepository) {
        this.feedbackRepository = feedbackRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    @CacheEvict(value = "feedback", allEntries = true)
    public void submit(PatientFeedback feedback) throws ServiceException {
        // Business rule: feedback text must not be empty, patient must exist
        if (feedback.getFeedback() == null || feedback.getFeedback().isBlank())
            throw new ServiceException("Feedback text cannot be empty.");
        if (!patientRepository.existsById(feedback.getPatientId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Cannot submit feedback: patient with id " + feedback.getPatientId() + " not found.");
        feedbackRepository.save(feedback);
    }

    @Transactional
    @CacheEvict(value = "feedback", allEntries = true)
    public void remove(long feedbackId) throws ServiceException {
        if (!feedbackRepository.existsById(feedbackId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Feedback with id " + feedbackId + " not found.");
        feedbackRepository.deleteById(feedbackId);
    }

    @Transactional(readOnly = true)
    @Cacheable("feedback")
    public List<PatientFeedback> getAll() {
        return feedbackRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "feedback", key = "#keyword")
    public List<PatientFeedback> search(String keyword) {
        return feedbackRepository.findByFeedbackContainingIgnoreCase(keyword);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "feedback", key = "#patientId")
    public List<PatientFeedback> getByPatient(long patientId) {
        return feedbackRepository.findByPatientId(patientId);
    }
}
