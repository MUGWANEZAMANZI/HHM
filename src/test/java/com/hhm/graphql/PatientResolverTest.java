package com.hhm.graphql;

import com.hhm.dto.PatientRequest;
import com.hhm.model.Patient;
import com.hhm.service.PatientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PatientResolverTest {

    @Mock PatientService patientService;
    @InjectMocks PatientResolver patientResolver;

    private static Patient patient(String name) {
        Patient p = new Patient(name, "0780000000");
        p.setEmail(name.toLowerCase() + "@hhm.rw");
        return p;
    }

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
    void patientsPaged_nullParams_usesDefaultPageZeroAndSizeTen() {
        given(patientService.getAll()).willReturn(List.of());

        var result = patientResolver.patientsPaged(null, null);

        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(10);
        then(patientService).should().getAll();
    }

    @Test
    void patientsPaged_customParams_slicesCorrectly() {
        // 7 patients, page=1, size=3 → second page contains patients 4,5,6
        List<Patient> all = List.of(
            patient("P1"), patient("P2"), patient("P3"),
            patient("P4"), patient("P5"), patient("P6"), patient("P7"));
        given(patientService.getAll()).willReturn(all);

        var result = patientResolver.patientsPaged(1, 3);

        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent()).extracting(Patient::getName)
            .containsExactly("P4", "P5", "P6");
    }

    @Test
    void patientsPaged_negativePageIndex_clampsToZero() {
        given(patientService.getAll()).willReturn(List.of());

        var result = patientResolver.patientsPaged(-1, 5);

        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(5);
    }

    @Test
    void patientsPaged_pageOutOfBounds_returnsEmptyContent() {
        given(patientService.getAll()).willReturn(List.of(patient("Only")));

        var result = patientResolver.patientsPaged(5, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
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
