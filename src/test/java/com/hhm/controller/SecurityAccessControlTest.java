package com.hhm.controller;

import com.hhm.model.Department;
import com.hhm.model.Doctor;
import com.hhm.model.User;
import com.hhm.repository.AdminRepository;
import com.hhm.repository.UserRepository;
import com.hhm.service.*;
import com.hhm.util.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the @PreAuthorize rules on every secured endpoint.
 *
 * Strategy
 * ─────────
 * • @WebMvcTest loads only the web layer (no DB, no full context).
 * • @Import(SecurityConfig) brings in the real SecurityFilterChain + @EnableMethodSecurity.
 * • All service/repo dependencies are @MockitoBean so controllers can be constructed.
 * • Authentication is injected with SecurityMockMvcRequestPostProcessors.user(...) —
 *   this bypasses the JWT filter and sets the SecurityContext directly for the test.
 * • The custom principal() helper produces a User whose id is readable by Spring EL
 *   expressions such as authentication.principal.id == #id (owner-only checks).
 */
@WebMvcTest({
    DoctorController.class,
    PatientController.class,
    AdminController.class,
    AppointmentController.class
})
@Import(SecurityConfig.class)
class SecurityAccessControlTest {

    @Autowired MockMvc mvc;

    // ── controller dependencies ───────────────────────────────────────────────
    @MockitoBean AuthService authService;
    @MockitoBean DoctorService doctorService;
    @MockitoBean PatientService patientService;
    @MockitoBean PatientAdmissionService admissionService;
    @MockitoBean AppointmentService appointmentService;
    @MockitoBean AdminRepository adminRepository;
    @MockitoBean UserRepository userRepository;

    // ── security infrastructure (required by SecurityConfig + JwtAuthenticationFilter) ─
    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;

    // ── stub shared returns so the service layer doesn't explode on allowed requests ──
    @BeforeEach
    void stubServices() {
        when(doctorService.getAll()).thenReturn(List.of());
        when(patientService.getAll(any())).thenReturn(Page.empty());
        when(userRepository.findAll()).thenReturn(List.of());
        when(adminRepository.findAll()).thenReturn(List.of());

        // updateDoctor controller calls doctorService.findById(departmentId).getDepartment()
        Department dept = new Department();
        Doctor stubDoctor = new Doctor("stub", dept, "LIC-STUB");
        when(doctorService.findById(anyLong())).thenReturn(stubDoctor);
    }

    /**
     * Builds a UserDetails principal that carries a specific id and authority.
     * The id is used by @PreAuthorize expressions like:
     *   authentication.principal.id == #id
     */
    private static UserDetails principal(long id, String authority) {
        return new User() {
            @Override public long getId()         { return id; }
            @Override public String getUsername() { return "u" + id + "@test.com"; }
            @Override public String getPassword() { return ""; }
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(authority));
            }
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/doctors  ─  no role restriction but requires authentication
    // ══════════════════════════════════════════════════════════════════════════

    @Test void listDoctors_anonymous_isDenied() throws Exception {
        mvc.perform(get("/api/v1/doctors"))
           .andExpect(status().is4xxClientError());
    }

    @Test void listDoctors_asDoctor_isOk() throws Exception {
        mvc.perform(get("/api/v1/doctors").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isOk());
    }

    @Test void listDoctors_asPatient_isOk() throws Exception {
        mvc.perform(get("/api/v1/doctors").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/v1/doctors  ─  ADMIN only
    // ══════════════════════════════════════════════════════════════════════════

    private static final String REGISTER_DOCTOR_BODY = """
            {"name":"Dr. X","email":"drx@example.com","password":"pass123",
             "type":"DOCTOR","licenseNumber":"LIC-001","departmentId":1}
            """;

    @Test void createDoctor_asAdmin_isCreated() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .with(user(principal(1L, "ADMIN")))
                .contentType("application/json")
                .content(REGISTER_DOCTOR_BODY))
           .andExpect(status().isCreated());
    }

    @Test void createDoctor_asDoctor_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .with(user(principal(2L, "DOCTOR")))
                .contentType("application/json")
                .content(REGISTER_DOCTOR_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void createDoctor_asPatient_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .with(user(principal(2L, "PATIENT")))
                .contentType("application/json")
                .content(REGISTER_DOCTOR_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void createDoctor_anonymous_isDenied() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .contentType("application/json")
                .content(REGISTER_DOCTOR_BODY))
           .andExpect(status().is4xxClientError());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUT /api/v1/doctors/{id}  ─  ADMIN or the doctor whose id matches
    // ══════════════════════════════════════════════════════════════════════════

    private static final String UPDATE_DOCTOR_BODY = """
            {"name":"Dr. Updated","licenseNumber":"LIC-999","departmentId":1}
            """;

    @Test void updateDoctor_asAdmin_isOk() throws Exception {
        mvc.perform(put("/api/v1/doctors/42")
                .with(user(principal(1L, "ADMIN")))
                .contentType("application/json")
                .content(UPDATE_DOCTOR_BODY))
           .andExpect(status().isOk());
    }

    @Test void updateDoctor_asOwningDoctor_isOk() throws Exception {
        // principal id (42) == path variable (42) → owner match → allowed
        mvc.perform(put("/api/v1/doctors/42")
                .with(user(principal(42L, "DOCTOR")))
                .contentType("application/json")
                .content(UPDATE_DOCTOR_BODY))
           .andExpect(status().isOk());
    }

    @Test void updateDoctor_asDifferentDoctor_isForbidden() throws Exception {
        // principal id (99) != path variable (42) → not the owner → denied
        mvc.perform(put("/api/v1/doctors/42")
                .with(user(principal(99L, "DOCTOR")))
                .contentType("application/json")
                .content(UPDATE_DOCTOR_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void updateDoctor_asPatient_isForbidden() throws Exception {
        mvc.perform(put("/api/v1/doctors/42")
                .with(user(principal(42L, "PATIENT")))
                .contentType("application/json")
                .content(UPDATE_DOCTOR_BODY))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/doctors/{id}  ─  ADMIN only
    // ══════════════════════════════════════════════════════════════════════════

    @Test void deleteDoctor_asAdmin_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/doctors/1").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isNoContent());
    }

    @Test void deleteDoctor_asDoctor_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/doctors/1").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    @Test void deleteDoctor_asReceptionist_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/doctors/1").with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/patients  ─  any authenticated user (anyRequest().authenticated())
    // ══════════════════════════════════════════════════════════════════════════

    @Test void listPatients_anonymous_isDenied() throws Exception {
        mvc.perform(get("/api/v1/patients"))
           .andExpect(status().is4xxClientError());
    }

    @Test void listPatients_asAdmin_isOk() throws Exception {
        mvc.perform(get("/api/v1/patients").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isOk());
    }

    @Test void listPatients_asDoctor_isOk() throws Exception {
        mvc.perform(get("/api/v1/patients").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isOk());
    }

    @Test void listPatients_asPatient_isOk() throws Exception {
        mvc.perform(get("/api/v1/patients").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUT /api/v1/patients/{id}  ─  ADMIN or the patient whose id matches
    // ══════════════════════════════════════════════════════════════════════════

    private static final String UPDATE_PATIENT_BODY = """
            {"name":"Alice","phone":"0788000001","email":"alice@example.com",
             "address":"KG 5 Ave","district":"Gasabo","sector":"Remera",
             "cell":"Rukiri","village":"Kabuye"}
            """;

    @Test void updatePatient_asAdmin_isOk() throws Exception {
        mvc.perform(put("/api/v1/patients/5")
                .with(user(principal(1L, "ADMIN")))
                .contentType("application/json")
                .content(UPDATE_PATIENT_BODY))
           .andExpect(status().isOk());
    }

    @Test void updatePatient_asOwningPatient_isOk() throws Exception {
        // principal id (5) == path variable (5) → owner → allowed
        mvc.perform(put("/api/v1/patients/5")
                .with(user(principal(5L, "PATIENT")))
                .contentType("application/json")
                .content(UPDATE_PATIENT_BODY))
           .andExpect(status().isOk());
    }

    @Test void updatePatient_asDifferentPatient_isForbidden() throws Exception {
        // principal id (99) != path variable (5) → not the owner → denied
        mvc.perform(put("/api/v1/patients/5")
                .with(user(principal(99L, "PATIENT")))
                .contentType("application/json")
                .content(UPDATE_PATIENT_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void updatePatient_asDoctor_isForbidden() throws Exception {
        mvc.perform(put("/api/v1/patients/5")
                .with(user(principal(5L, "DOCTOR")))
                .contentType("application/json")
                .content(UPDATE_PATIENT_BODY))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/patients/{id}  ─  ADMIN only
    // ══════════════════════════════════════════════════════════════════════════

    @Test void deletePatient_asAdmin_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/patients/1").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isNoContent());
    }

    @Test void deletePatient_asPatient_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/patients/1").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isForbidden());
    }

    @Test void deletePatient_asDoctor_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/patients/1").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/admin/**  ─  ADMIN only (class-level @PreAuthorize on AdminController)
    // ══════════════════════════════════════════════════════════════════════════

    @Test void listAllUsers_asAdmin_isOk() throws Exception {
        mvc.perform(get("/api/v1/admin/users").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isOk());
    }

    @Test void listAllUsers_asDoctor_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/users").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    @Test void listAllUsers_asPatient_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/users").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isForbidden());
    }

    @Test void listAllUsers_asReceptionist_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/users").with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isForbidden());
    }

    @Test void listAllUsers_anonymous_isDenied() throws Exception {
        mvc.perform(get("/api/v1/admin/users"))
           .andExpect(status().is4xxClientError());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/v1/appointments  ─  PATIENT or RECEPTIONIST
    // ══════════════════════════════════════════════════════════════════════════

    private static final String BOOK_APPOINTMENT_BODY = """
            {"patientId":1,"doctorId":2,"appointmentDate":"2027-06-01T10:00:00"}
            """;

    @Test void bookAppointment_asPatient_isCreated() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                .with(user(principal(1L, "PATIENT")))
                .contentType("application/json")
                .content(BOOK_APPOINTMENT_BODY))
           .andExpect(status().isCreated());
    }

    @Test void bookAppointment_asReceptionist_isCreated() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                .with(user(principal(1L, "RECEPTIONIST")))
                .contentType("application/json")
                .content(BOOK_APPOINTMENT_BODY))
           .andExpect(status().isCreated());
    }

    @Test void bookAppointment_asDoctor_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                .with(user(principal(1L, "DOCTOR")))
                .contentType("application/json")
                .content(BOOK_APPOINTMENT_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void bookAppointment_asAdmin_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                .with(user(principal(1L, "ADMIN")))
                .contentType("application/json")
                .content(BOOK_APPOINTMENT_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void bookAppointment_anonymous_isDenied() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                .contentType("application/json")
                .content(BOOK_APPOINTMENT_BODY))
           .andExpect(status().is4xxClientError());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/appointments/{id}/cancel  ─  ADMIN or RECEPTIONIST
    // ══════════════════════════════════════════════════════════════════════════

    @Test void cancelAppointment_asAdmin_isOk() throws Exception {
        mvc.perform(patch("/api/v1/appointments/1/cancel")
                .with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isOk());
    }

    @Test void cancelAppointment_asReceptionist_isOk() throws Exception {
        mvc.perform(patch("/api/v1/appointments/1/cancel")
                .with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isOk());
    }

    @Test void cancelAppointment_asPatient_isForbidden() throws Exception {
        mvc.perform(patch("/api/v1/appointments/1/cancel")
                .with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isForbidden());
    }

    @Test void cancelAppointment_asDoctor_isForbidden() throws Exception {
        mvc.perform(patch("/api/v1/appointments/1/cancel")
                .with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/appointments/{id}  ─  ADMIN only
    // ══════════════════════════════════════════════════════════════════════════

    @Test void deleteAppointment_asAdmin_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/appointments/1")
                .with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isNoContent());
    }

    @Test void deleteAppointment_asReceptionist_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/appointments/1")
                .with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isForbidden());
    }

    @Test void deleteAppointment_asPatient_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/appointments/1")
                .with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isForbidden());
    }
}
