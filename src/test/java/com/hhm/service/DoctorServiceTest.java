package com.hhm.service;

import com.hhm.dto.DoctorRequest;
import com.hhm.model.Appointment;
import com.hhm.model.Doctor;
import com.hhm.repository.AppointmentRepository;
import com.hhm.repository.DepartmentRepository;
import com.hhm.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hhm.model.Department;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock DoctorRepository doctorRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock AppointmentRepository appointmentRepository;
    @InjectMocks DoctorService doctorService;

    private DoctorRequest req(String name, long deptId, String license) {
        DoctorRequest r = new DoctorRequest();
        r.setName(name);
        r.setDepartmentId(deptId);
        r.setLicenseNumber(license);
        return r;
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    void register_success_savesDoctor() {
        given(departmentRepository.existsById(1L)).willReturn(true);
        given(departmentRepository.findById(1L)).willReturn(Optional.of(new Department()));

        doctorService.register(req("Dr. Alice", 1L, "LIC-001"));

        then(doctorRepository).should().save(any(Doctor.class));
    }

    @Test
    void register_departmentNotFound_throws() {
        given(departmentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> doctorService.register(req("Dr. Alice", 99L, "LIC-001")))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    @Test
    void register_duplicateLicense_throwsConflict() {
        given(departmentRepository.existsById(1L)).willReturn(true);
        given(doctorRepository.existsByLicenseNumberIgnoreCase("LIC-001")).willReturn(true);

        assertThatThrownBy(() -> doctorService.register(req("Dr. Alice", 1L, "LIC-001")))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("LIC-001")
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    @Test
    void register_licenseCheckIsCaseInsensitive() {
        given(departmentRepository.existsById(1L)).willReturn(true);
        given(doctorRepository.existsByLicenseNumberIgnoreCase("LIC-001")).willReturn(true);

        assertThatThrownBy(() -> doctorService.register(req("Dr. Alice", 1L, "LIC-001")))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    // ─── remove ──────────────────────────────────────────────────────────────

    @Test
    void remove_success_deletesDoctor() {
        given(doctorRepository.existsById(1L)).willReturn(true);
        given(appointmentRepository.findByDoctor_Id(1L)).willReturn(List.of());

        doctorService.remove(1L);

        then(doctorRepository).should().deleteById(1L);
    }

    @Test
    void remove_notFound_throws() {
        given(doctorRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> doctorService.remove(99L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    @Test
    void remove_hasScheduledAppointments_throwsConflict() {
        given(doctorRepository.existsById(1L)).willReturn(true);
        Appointment scheduled = new Appointment();
        scheduled.setAppointmentDate(LocalDateTime.now().plusDays(1));
        scheduled.setStatus("Scheduled");
        given(appointmentRepository.findByDoctor_Id(1L)).willReturn(List.of(scheduled));

        assertThatThrownBy(() -> doctorService.remove(1L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    @Test
    void remove_onlyCancelledAppointments_succeeds() {
        given(doctorRepository.existsById(1L)).willReturn(true);
        Appointment cancelled = new Appointment();
        cancelled.setAppointmentDate(LocalDateTime.now().minusDays(1));
        cancelled.setStatus("Cancelled");
        given(appointmentRepository.findByDoctor_Id(1L)).willReturn(List.of(cancelled));

        assertThatCode(() -> doctorService.remove(1L)).doesNotThrowAnyException();
        then(doctorRepository).should().deleteById(1L);
    }
}
