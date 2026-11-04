package com.hhm.graphql;

import com.hhm.dto.PatientRequest;
import com.hhm.model.Patient;
import com.hhm.service.PatientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PatientResolverTest {

    @Mock PatientService patientService;
    @InjectMocks PatientResolver patientResolver;

    // ─── patients query ──────────────────────────────────────────────────────

    @Test
    void patients_nullSearch_delegatesToGetAll() {
        Patient p = new Patient("Alice", "0780000001");
        p.setEmail("alice@hhm.rw");
        given(patientService.getAll()).willReturn(List.of(p));

        List<Patient> result = patientResolver.patients(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
        then(patientService).should().getAll();
        then(patientService).should(never()).search(any());
    }

    @Test
    void patients_blankSearch_delegatesToGetAll() {
        given(patientService.getAll()).willReturn(List.of());

        patientResolver.patients("   ");

        then(patientService).should().getAll();
        then(patientService).should(never()).search(any());
    }

    @Test
    void patients_withSearch_delegatesToSearch() {
        Patient p = new Patient("Alice", "0780000001");
        p.setEmail("alice@hhm.rw");
        given(patientService.search("Alice")).willReturn(List.of(p));

        List<Patient> result = patientResolver.patients("Alice");

        assertThat(result).hasSize(1);
        then(patientService).should().search("Alice");
        then(patientService).should(never()).getAll();
    }

    // ─── patientsPaged query ─────────────────────────────────────────────────

    @Test
    void patientsPaged_nullParams_usesDefaultPageAndSize() {
        given(patientService.getAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        patientResolver.patientsPaged(null, null);

        then(patientService).should().getAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void patientsPaged_customParams_passesCorrectPageable() {
        given(patientService.getAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        patientResolver.patientsPaged(2, 5);

        then(patientService).should().getAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void patientsPaged_negativePageIndex_clampsToZero() {
        given(patientService.getAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        patientResolver.patientsPaged(-1, 5);

        then(patientService).should().getAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
    }

    // ─── registerPatient mutation ─────────────────────────────────────────────

    @Test
    void registerPatient_success_returnsConfirmationMessage() {
        PatientRequest req = new PatientRequest();
        req.setName("Alice");
        req.setPhone("0780000001");
        req.setEmail("alice@hhm.rw");
        given(patientService.register(any(), any(), any(), any(), any(), any(), any(), any()))
            .willReturn(5L);

        String result = patientResolver.registerPatient(req);

        assertThat(result).isEqualTo("Patient registered with id 5");
    }

    @Test
    void registerPatient_passesAllFieldsToService() {
        PatientRequest req = new PatientRequest();
        req.setName("Alice");
        req.setPhone("0780000001");
        req.setEmail("alice@hhm.rw");
        req.setAddress("KG 5 Ave");
        req.setDistrict("Gasabo");
        req.setSector("Remera");
        req.setCell("Rukiri");
        req.setVillage("Kabuye");
        given(patientService.register("Alice", "0780000001", "alice@hhm.rw",
            "KG 5 Ave", "Gasabo", "Remera", "Rukiri", "Kabuye")).willReturn(1L);

        patientResolver.registerPatient(req);

        then(patientService).should().register(
            "Alice", "0780000001", "alice@hhm.rw",
            "KG 5 Ave", "Gasabo", "Remera", "Rukiri", "Kabuye");
    }

    // ─── deletePatient mutation ───────────────────────────────────────────────

    @Test
    void deletePatient_callsRemoveAndReturnsTrue() {
        willDoNothing().given(patientService).remove(1L);

        boolean result = patientResolver.deletePatient(1L);

        assertThat(result).isTrue();
        then(patientService).should().remove(1L);
    }
}
