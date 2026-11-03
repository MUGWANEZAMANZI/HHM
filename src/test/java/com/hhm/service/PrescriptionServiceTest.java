package com.hhm.service;

import com.hhm.model.Doctor;
import com.hhm.model.Patient;
import com.hhm.model.Prescription;
import com.hhm.repository.DoctorRepository;
import com.hhm.repository.PatientRepository;
import com.hhm.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock PrescriptionRepository prescriptionRepository;
    @Mock PatientRepository patientRepository;
    @Mock DoctorRepository doctorRepository;
    @InjectMocks PrescriptionService prescriptionService;

    private Patient stubPatient(long id) {
        Patient p = new Patient("Name", "0780000001");
        p.setId(id);
        return p;
    }

    private Doctor stubDoctor(long id) {
        Doctor d = new Doctor("Dr. Name", new com.hhm.model.Department(), "LIC");
        d.setId(id);
        return d;
    }

    // ─── issue ───────────────────────────────────────────────────────────────

    @Test
    void issue_success_savesPrescription() {
        given(patientRepository.findById(1L)).willReturn(Optional.of(stubPatient(1L)));
        given(doctorRepository.findById(2L)).willReturn(Optional.of(stubDoctor(2L)));

        prescriptionService.issue(1L, 2L, LocalDateTime.now(), "Active", "Take after meals");

        then(prescriptionRepository).should().save(any(Prescription.class));
    }

    @Test
    void issue_patientNotFound_throws() {
        given(patientRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.issue(99L, 2L, LocalDateTime.now(), "Active", null))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    // ─── cancel ──────────────────────────────────────────────────────────────

    @Test
    void cancel_active_setsStatusAndSaves() {
        Prescription p = new Prescription(stubPatient(1L), stubDoctor(2L), LocalDateTime.now(), "Active", null);
        given(prescriptionRepository.findById(1L)).willReturn(Optional.of(p));

        prescriptionService.cancel(1L);

        assertThat(p.getStatus()).isEqualTo("Cancelled");
        then(prescriptionRepository).should().save(p);
    }

    @Test
    void cancel_notFound_throws() {
        given(prescriptionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.cancel(99L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    @Test
    void cancel_alreadyCancelled_throwsConflict() {
        Prescription p = new Prescription(stubPatient(1L), stubDoctor(2L), LocalDateTime.now(), "Cancelled", null);
        given(prescriptionRepository.findById(1L)).willReturn(Optional.of(p));

        assertThatThrownBy(() -> prescriptionService.cancel(1L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    // ─── remove ──────────────────────────────────────────────────────────────

    @Test
    void remove_success_deletesPrescription() {
        given(prescriptionRepository.existsById(1L)).willReturn(true);

        prescriptionService.remove(1L);

        then(prescriptionRepository).should().deleteById(1L);
    }

    @Test
    void remove_notFound_throws() {
        given(prescriptionRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> prescriptionService.remove(99L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    void update_notFound_throws() {
        given(prescriptionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.update(99L, 1L, 2L, LocalDateTime.now(), "Active", null))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }
}
