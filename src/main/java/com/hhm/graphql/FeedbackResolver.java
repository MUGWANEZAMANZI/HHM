package com.hhm.graphql;

import com.hhm.dto.FeedbackRequest;
import com.hhm.model.PatientFeedback;
import com.hhm.service.PatientFeedbackService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class FeedbackResolver {

    private final PatientFeedbackService feedbackService;

    public FeedbackResolver(PatientFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @QueryMapping
    public List<PatientFeedback> feedback(@Argument String search) {
        if (search != null && !search.isBlank())
            return feedbackService.search(search);
        return feedbackService.getAll();
    }

    @PreAuthorize("hasAuthority('RECEPTIONIST') or hasAuthority('PATIENT')")
    @MutationMapping
    public String submitFeedback(@Argument @Valid FeedbackRequest input) {
        feedbackService.submit(input.getPatientId(), input.getFeedback(),
                               input.getPrivateNotes(), LocalDateTime.now());
        return "Feedback submitted successfully";
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('NURSE') or hasAuthority('RECEPTIONIST')")
    @MutationMapping
    public boolean deleteFeedback(@Argument Long id) {
        feedbackService.remove(id);
        return true;
    }
}
