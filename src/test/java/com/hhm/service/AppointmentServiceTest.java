package com.hhm.service;

import com.hhm.model.Appointment;
import com.hhm.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @InjectMocks AppointmentService appointmentService;

    // ─── book ────────────────────────────────────────────────────────────────

    @Test
    void book_futureDate_savesAppointment() {
        Appointment a = new Appointment(1L, 2L, LocalDateTime.now().plusDays(1), "Scheduled");

        appointmentService.book(a);

        then(appointmentRepository).should().save(a);
    }

    @Test
    void book_pastDate_throws() {
        Appointment a = new Appointment(1L, 2L, LocalDateTime.now().minusHours(1), "Scheduled");

        assertThatThrownBy(() -> appointmentService.book(a))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("past");
    }

    // ─── cancel ──────────────────────────────────────────────────────────────

    @Test
    void cancel_scheduledAppointment_setsStatusAndSaves() {
        Appointment existing = new Appointment(1L, 2L, LocalDateTime.now().plusDays(1), "Scheduled");
        given(appointmentRepository.findById(1L)).willReturn(Optional.of(existing));

        appointmentService.cancel(1L);

        assertThat(existing.getStatus()).isEqualTo("Cancelled");
        then(appointmentRepository).should().save(existing);
    }

    @Test
    void cancel_notFound_throws() {
        given(appointmentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancel(99L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    @Test
    void cancel_alreadyCancelled_throwsConflict() {
        Appointment existing = new Appointment(1L, 2L, LocalDateTime.now().minusDays(1), "Cancelled");
        given(appointmentRepository.findById(1L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> appointmentService.cancel(1L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    // ─── remove ──────────────────────────────────────────────────────────────

    @Test
    void remove_success_deletesAppointment() {
        given(appointmentRepository.existsById(1L)).willReturn(true);

        appointmentService.remove(1L);

        then(appointmentRepository).should().deleteById(1L);
    }

    @Test
    void remove_notFound_throws() {
        given(appointmentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> appointmentService.remove(99L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    // ─── reschedule ──────────────────────────────────────────────────────────

    @Test
    void reschedule_futureDate_saves() {
        Appointment a = new Appointment(1L, 2L, LocalDateTime.now().plusDays(2), "Rescheduled");
        given(appointmentRepository.existsById(0L)).willReturn(true);

        appointmentService.reschedule(a);

        then(appointmentRepository).should().save(a);
    }

    @Test
    void reschedule_pastDate_throws() {
        Appointment a = new Appointment(1L, 2L, LocalDateTime.now().minusDays(1), "Rescheduled");

        assertThatThrownBy(() -> appointmentService.reschedule(a))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("past");
    }
}
