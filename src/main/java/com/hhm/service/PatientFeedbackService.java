package com.hhm.service;

import com.hhm.model.Patient;
import com.hhm.model.PatientFeedback;
import com.hhm.repository.PatientFeedbackRepository;
import com.hhm.repository.PatientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public void submit(long patientId, String feedback, String privateNotes, LocalDateTime issuedAt)
            throws ServiceException {
        if (feedback == null || feedback.isBlank())
            throw new ServiceException("Feedback text cannot be empty.");
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Cannot submit feedback: patient with id " + patientId + " not found."));
        feedbackRepository.save(new PatientFeedback(patient, feedback, privateNotes, issuedAt));
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

}
