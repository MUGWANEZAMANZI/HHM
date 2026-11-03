package com.hhm.service;

import com.hhm.model.Patient;
import com.hhm.model.PatientAddress;
import com.hhm.repository.PatientAddressRepository;
import com.hhm.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock PatientRepository patientRepository;
    @Mock PatientAddressRepository addressRepository;
    @InjectMocks PatientService patientService;

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    void register_success_returnsGeneratedId() {
        given(patientRepository.findByEmail("alice@hhm.rw")).willReturn(Optional.empty());
        given(patientRepository.findByPhone("0780000001")).willReturn(Optional.empty());
        Patient saved = new Patient("Alice", "0780000001", "alice@hhm.rw");
        saved.setId(1L);
        given(patientRepository.save(any())).willReturn(saved);

        long id = patientService.register("Alice", "0780000001", "alice@hhm.rw", null, null, null, null, null);

        assertThat(id).isEqualTo(1L);
    }

    @Test
    void register_withDistrict_savesAddress() {
        given(patientRepository.findByEmail("alice@hhm.rw")).willReturn(Optional.empty());
        given(patientRepository.findByPhone("0780000001")).willReturn(Optional.empty());
        Patient saved = new Patient("Alice", "0780000001", "alice@hhm.rw");
        saved.setId(1L);
        given(patientRepository.save(any())).willReturn(saved);

        patientService.register("Alice", "0780000001", "alice@hhm.rw", "KG 5 Ave", "Gasabo", "Remera", "Rukiri", "Kabuye");

        then(addressRepository).should().save(any(PatientAddress.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        given(patientRepository.findByEmail("alice@hhm.rw"))
            .willReturn(Optional.of(new Patient("Other", "0780000002", "alice@hhm.rw")));

        assertThatThrownBy(() ->
            patientService.register("Alice", "0780000001", "alice@hhm.rw", null, null, null, null, null))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("alice@hhm.rw")
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    @Test
    void register_duplicatePhone_throwsConflict() {
        given(patientRepository.findByEmail("alice@hhm.rw")).willReturn(Optional.empty());
        given(patientRepository.findByPhone("0780000001"))
            .willReturn(Optional.of(new Patient("Other", "0780000001", "other@hhm.rw")));

        assertThatThrownBy(() ->
            patientService.register("Alice", "0780000001", "alice@hhm.rw", null, null, null, null, null))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("0780000001")
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    void update_success() {
        Patient patient = new Patient("Alice", "0780000001", "alice@hhm.rw");
        patient.setId(1L);
        given(patientRepository.existsById(1L)).willReturn(true);
        given(patientRepository.findByEmail("alice@hhm.rw")).willReturn(Optional.of(patient));

        patientService.update(patient, null, null, null, null);

        then(patientRepository).should().save(patient);
    }

    @Test
    void update_notFound_throws() {
        Patient patient = new Patient("Alice", "0780000001", "alice@hhm.rw");
        patient.setId(99L);
        given(patientRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> patientService.update(patient, null, null, null, null))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    @Test
    void update_emailTakenByOtherPatient_throwsConflict() {
        Patient patient = new Patient("Alice", "0780000001", "alice@hhm.rw");
        patient.setId(1L);
        Patient other = new Patient("Bob", "0780000002", "alice@hhm.rw");
        other.setId(2L);
        given(patientRepository.existsById(1L)).willReturn(true);
        given(patientRepository.findByEmail("alice@hhm.rw")).willReturn(Optional.of(other));

        assertThatThrownBy(() -> patientService.update(patient, null, null, null, null))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("alice@hhm.rw")
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    @Test
    void update_emailBelongsToSamePatient_doesNotThrow() {
        Patient patient = new Patient("Alice", "0780000001", "alice@hhm.rw");
        patient.setId(1L);
        given(patientRepository.existsById(1L)).willReturn(true);
        // same patient found by email → no conflict
        given(patientRepository.findByEmail("alice@hhm.rw")).willReturn(Optional.of(patient));

        assertThatCode(() -> patientService.update(patient, null, null, null, null))
            .doesNotThrowAnyException();
    }

    // ─── remove ──────────────────────────────────────────────────────────────

    @Test
    void remove_success_deletesAddressThenPatient() {
        given(patientRepository.existsById(1L)).willReturn(true);

        patientService.remove(1L);

        then(addressRepository).should().deleteByPatientId(1L);
        then(patientRepository).should().deleteById(1L);
    }

    @Test
    void remove_notFound_throws() {
        given(patientRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> patientService.remove(99L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    // ─── read ────────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsList() {
        given(patientRepository.findAll()).willReturn(
            List.of(new Patient("Alice", "0780000001", "alice@hhm.rw")));

        List<Patient> result = patientService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void search_returnsMatchingPatients() {
        Patient alice = new Patient("Alice", "0780000001", "alice@hhm.rw");
        given(patientRepository.search("Alice")).willReturn(List.of(alice));

        List<Patient> result = patientService.search("Alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }
}
