package com.hhm.service;

import com.hhm.model.Patient;
import com.hhm.model.PatientFeedback;
import com.hhm.repository.PatientFeedbackRepository;
import com.hhm.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PatientFeedbackServiceTest {

    @Mock PatientFeedbackRepository feedbackRepository;
    @Mock PatientRepository patientRepository;
    @InjectMocks PatientFeedbackService feedbackService;

    // ─── submit ──────────────────────────────────────────────────────────────

    @Test
    void submit_success_savesFeedback() {
        given(patientRepository.findById(1L)).willReturn(Optional.of(new Patient("Alice", "078")));

        feedbackService.submit(1L, "Great service", null, LocalDateTime.now());

        then(feedbackRepository).should().save(any(PatientFeedback.class));
    }

    @Test
    void submit_blankFeedback_throws() {
        assertThatThrownBy(() -> feedbackService.submit(1L, "   ", null, LocalDateTime.now()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("empty");
    }

    @Test
    void submit_nullFeedback_throws() {
        assertThatThrownBy(() -> feedbackService.submit(1L, null, null, LocalDateTime.now()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("empty");
    }

    @Test
    void submit_patientNotFound_throws() {
        assertThatThrownBy(() -> feedbackService.submit(99L, "Great service", null, LocalDateTime.now()))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    // ─── remove ──────────────────────────────────────────────────────────────

    @Test
    void remove_success_deletesFeedback() {
        given(feedbackRepository.existsById(1L)).willReturn(true);

        feedbackService.remove(1L);

        then(feedbackRepository).should().deleteById(1L);
    }

    @Test
    void remove_notFound_throws() {
        given(feedbackRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> feedbackService.remove(99L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    // ─── read ────────────────────────────────────────────────────────────────

    @Test
    void getAll_delegatesToRepository() {
        given(feedbackRepository.findAll()).willReturn(List.of());

        feedbackService.getAll();

        then(feedbackRepository).should().findAll();
    }

    @Test
    void search_delegatesToRepository() {
        given(feedbackRepository.findByFeedbackContainingIgnoreCase("great")).willReturn(List.of());

        feedbackService.search("great");

        then(feedbackRepository).should().findByFeedbackContainingIgnoreCase("great");
    }
}
