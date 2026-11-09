package com.hhm.service;

import com.hhm.model.Appointment;
import com.hhm.model.Doctor;
import com.hhm.model.Patient;
import com.hhm.repository.AppointmentRepository;
import com.hhm.repository.DoctorRepository;
import com.hhm.repository.PatientRepository;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock PatientRepository patientRepository;
    AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository, doctorRepository, patientRepository, Runnable::run);
    }

    // ─── book ────────────────────────────────────────────────────────────────

    @Test
    void book_futureDate_savesAppointment() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        given(patientRepository.findById(1L))
                .willReturn(Optional.of(new Patient("Alice", "078")));
        given(doctorRepository.findById(2L))
                .willReturn(Optional.of(new Doctor("Dr. Bob", new com.hhm.model.Department(), "LIC")));

        appointmentService.book(1L, 2L, future, "Scheduled");

        then(appointmentRepository).should().save(any(Appointment.class));
    }

    @Test
    void book_pastDate_throws() {
        LocalDateTime past = LocalDateTime.now().minusHours(1);

        assertThatThrownBy(() -> appointmentService.book(1L, 2L, past, "Scheduled"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("past");
    }

    // ─── cancel ──────────────────────────────────────────────────────────────

    @Test
    void cancel_scheduledAppointment_setsStatusAndSaves() {
        Appointment existing = new Appointment();
        existing.setAppointmentDate(LocalDateTime.now().plusDays(1));
        existing.setStatus("Scheduled");
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
        Appointment existing = new Appointment();
        existing.setAppointmentDate(LocalDateTime.now().minusDays(1));
        existing.setStatus("Cancelled");
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
        LocalDateTime future = LocalDateTime.now().plusDays(2);
        Appointment existing = new Appointment();
        given(appointmentRepository.findById(1L)).willReturn(Optional.of(existing));
        given(patientRepository.findById(1L))
                .willReturn(Optional.of(new Patient("Alice", "078")));
        given(doctorRepository.findById(2L))
                .willReturn(Optional.of(new Doctor("Dr. Bob", new com.hhm.model.Department(), "LIC")));

        appointmentService.reschedule(1L, 1L, 2L, future);

        then(appointmentRepository).should().save(any(Appointment.class));
    }

    @Test
    void reschedule_pastDate_throws() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> appointmentService.reschedule(1L, 1L, 2L, past))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("past");
    }
}
