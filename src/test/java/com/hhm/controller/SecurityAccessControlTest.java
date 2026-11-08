package com.hhm.controller;

import com.hhm.model.Department;
import com.hhm.model.Doctor;
import com.hhm.model.User;
import com.hhm.repository.AdminRepository;
import com.hhm.repository.UserRepository;
import com.hhm.service.*;
import com.hhm.util.RateLimitFilter;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
 *   this bypasses the JWT filter and sets the SecurityContext directly.
 * • The principal() helper produces a User whose id is used in Spring EL owner checks
 *   such as: authentication.principal.id == #id
 */
@WebMvcTest({
    DoctorController.class,
    PatientController.class,
    AdminController.class,
    AppointmentController.class,
    FeedbackController.class,
    InventoryController.class
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
    @MockitoBean PatientFeedbackService feedbackService;
    @MockitoBean MedicalInventoryService inventoryService;
    @MockitoBean AdminRepository adminRepository;
    @MockitoBean UserRepository userRepository;

    // ── security infrastructure ───────────────────────────────────────────────
    // These satisfy the constructor/filter dependencies that SecurityConfig pulls in.
    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean RedisSecurityService redisSecurityService;
    // SecurityConfig takes OAuth2UserSaveService as a constructor arg — must be present.
    @MockitoBean OAuth2UserSaveService oAuth2UserSaveService;
    // oauth2Login() DSL requires ClientRegistrationRepository to be in the context.
    @MockitoBean ClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void stubServices() {
        // Doctors
        when(doctorService.getAll()).thenReturn(List.of());
        when(doctorService.search(anyString())).thenReturn(List.of());
        Department dept = new Department();
        Doctor stubDoctor = new Doctor("stub", dept, "LIC-STUB");
        when(doctorService.findById(anyLong())).thenReturn(stubDoctor);

        // Patients
        when(patientService.getAll(any())).thenReturn(Page.empty());
        when(patientService.search(anyString())).thenReturn(List.of());

        // Admin
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByUserTypeIsNull()).thenReturn(List.of());
        when(adminRepository.findAll()).thenReturn(List.of());

        // Appointments
        when(appointmentService.getAll()).thenReturn(List.of());
        when(appointmentService.findByStatus(anyString())).thenReturn(List.of());

        // Feedback
        when(feedbackService.getAll()).thenReturn(List.of());
        when(feedbackService.search(anyString())).thenReturn(List.of());

        // Inventory
        when(inventoryService.getAll(any())).thenReturn(Page.empty());
        when(inventoryService.getLowStock()).thenReturn(List.of());
        when(inventoryService.search(anyString())).thenReturn(List.of());
    }

    /** Builds a UserDetails principal with a specific id and authority. */
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
    // GET /api/v1/doctors  —  any authenticated user
    // ══════════════════════════════════════════════════════════════════════════

    @Test void listDoctors_anonymous_is401() throws Exception {
        mvc.perform(get("/api/v1/doctors"))
           .andExpect(status().isUnauthorized());
    }

    @Test void listDoctors_asPatient_isOk() throws Exception {
        mvc.perform(get("/api/v1/doctors").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isOk());
    }

    @Test void listDoctors_asDoctor_isOk() throws Exception {
        mvc.perform(get("/api/v1/doctors").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isOk());
    }

    @Test void listDoctors_asAdmin_isOk() throws Exception {
        mvc.perform(get("/api/v1/doctors").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/v1/doctors  —  ADMIN only
    // ══════════════════════════════════════════════════════════════════════════

    private static final String CREATE_DOCTOR_BODY = """
            {"name":"Dr. X","email":"drx@example.com","password":"pass123",
             "type":"DOCTOR","licenseNumber":"LIC-001","departmentId":1}
            """;

    @Test void createDoctor_asAdmin_isCreated() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .with(user(principal(1L, "ADMIN")))
                .contentType("application/json")
                .content(CREATE_DOCTOR_BODY))
           .andExpect(status().isCreated());
    }

    @Test void createDoctor_asDoctor_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .with(user(principal(2L, "DOCTOR")))
                .contentType("application/json")
                .content(CREATE_DOCTOR_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void createDoctor_asNurse_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .with(user(principal(2L, "NURSE")))
                .contentType("application/json")
                .content(CREATE_DOCTOR_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void createDoctor_asPatient_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .with(user(principal(2L, "PATIENT")))
                .contentType("application/json")
                .content(CREATE_DOCTOR_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void createDoctor_anonymous_is401() throws Exception {
        mvc.perform(post("/api/v1/doctors")
                .contentType("application/json")
                .content(CREATE_DOCTOR_BODY))
           .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUT /api/v1/doctors/{id}  —  ADMIN or the owning DOCTOR
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
        mvc.perform(put("/api/v1/doctors/42")
                .with(user(principal(42L, "DOCTOR")))
                .contentType("application/json")
                .content(UPDATE_DOCTOR_BODY))
           .andExpect(status().isOk());
    }

    @Test void updateDoctor_asDifferentDoctor_isForbidden() throws Exception {
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

    @Test void updateDoctor_asNurse_isForbidden() throws Exception {
        mvc.perform(put("/api/v1/doctors/42")
                .with(user(principal(1L, "NURSE")))
                .contentType("application/json")
                .content(UPDATE_DOCTOR_BODY))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/doctors/{id}  —  ADMIN only
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

    @Test void deleteDoctor_asNurse_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/doctors/1").with(user(principal(1L, "NURSE"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/patients  —  any authenticated user
    // ══════════════════════════════════════════════════════════════════════════

    @Test void listPatients_anonymous_is401() throws Exception {
        mvc.perform(get("/api/v1/patients"))
           .andExpect(status().isUnauthorized());
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
    // PUT /api/v1/patients/{id}  —  ADMIN or the owning PATIENT
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
        mvc.perform(put("/api/v1/patients/5")
                .with(user(principal(5L, "PATIENT")))
                .contentType("application/json")
                .content(UPDATE_PATIENT_BODY))
           .andExpect(status().isOk());
    }

    @Test void updatePatient_asDifferentPatient_isForbidden() throws Exception {
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

    @Test void updatePatient_asNurse_isForbidden() throws Exception {
        mvc.perform(put("/api/v1/patients/5")
                .with(user(principal(1L, "NURSE")))
                .contentType("application/json")
                .content(UPDATE_PATIENT_BODY))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/patients/{id}  —  ADMIN only
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

    @Test void deletePatient_asReceptionist_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/patients/1").with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/admin/**  —  ADMIN only (class-level @PreAuthorize)
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

    @Test void listAllUsers_asNurse_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/users").with(user(principal(1L, "NURSE"))))
           .andExpect(status().isForbidden());
    }

    @Test void listAllUsers_asReceptionist_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/users").with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isForbidden());
    }

    @Test void listAllUsers_anonymous_is401() throws Exception {
        mvc.perform(get("/api/v1/admin/users"))
           .andExpect(status().isUnauthorized());
    }

    @Test void listPendingUsers_asAdmin_isOk() throws Exception {
        mvc.perform(get("/api/v1/admin/users/pending").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isOk());
    }

    @Test void listPendingUsers_asDoctor_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/users/pending").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/v1/appointments  —  PATIENT or RECEPTIONIST
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

    @Test void bookAppointment_asNurse_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                .with(user(principal(1L, "NURSE")))
                .contentType("application/json")
                .content(BOOK_APPOINTMENT_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void bookAppointment_anonymous_is401() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                .contentType("application/json")
                .content(BOOK_APPOINTMENT_BODY))
           .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/appointments/{id}/cancel  —  ADMIN or RECEPTIONIST
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

    @Test void cancelAppointment_asNurse_isForbidden() throws Exception {
        mvc.perform(patch("/api/v1/appointments/1/cancel")
                .with(user(principal(1L, "NURSE"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/appointments/{id}  —  ADMIN only
    // ══════════════════════════════════════════════════════════════════════════

    @Test void deleteAppointment_asAdmin_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/appointments/1").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isNoContent());
    }

    @Test void deleteAppointment_asReceptionist_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/appointments/1").with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isForbidden());
    }

    @Test void deleteAppointment_asPatient_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/appointments/1").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isForbidden());
    }

    @Test void deleteAppointment_asDoctor_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/appointments/1").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/v1/feedback  —  PATIENT or RECEPTIONIST
    // ══════════════════════════════════════════════════════════════════════════

    private static final String FEEDBACK_BODY = """
            {"patientId":1,"feedback":"Great service","privateNotes":null}
            """;

    @Test void submitFeedback_asPatient_isCreated() throws Exception {
        mvc.perform(post("/api/v1/feedback")
                .with(user(principal(1L, "PATIENT")))
                .contentType("application/json")
                .content(FEEDBACK_BODY))
           .andExpect(status().isCreated());
    }

    @Test void submitFeedback_asReceptionist_isCreated() throws Exception {
        mvc.perform(post("/api/v1/feedback")
                .with(user(principal(1L, "RECEPTIONIST")))
                .contentType("application/json")
                .content(FEEDBACK_BODY))
           .andExpect(status().isCreated());
    }

    @Test void submitFeedback_asDoctor_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/feedback")
                .with(user(principal(1L, "DOCTOR")))
                .contentType("application/json")
                .content(FEEDBACK_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void submitFeedback_asAdmin_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/feedback")
                .with(user(principal(1L, "ADMIN")))
                .contentType("application/json")
                .content(FEEDBACK_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void submitFeedback_asNurse_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/feedback")
                .with(user(principal(1L, "NURSE")))
                .contentType("application/json")
                .content(FEEDBACK_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void submitFeedback_anonymous_is401() throws Exception {
        mvc.perform(post("/api/v1/feedback")
                .contentType("application/json")
                .content(FEEDBACK_BODY))
           .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/feedback/{id}  —  ADMIN, NURSE, or RECEPTIONIST
    // ══════════════════════════════════════════════════════════════════════════

    @Test void deleteFeedback_asAdmin_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/feedback/1").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isNoContent());
    }

    @Test void deleteFeedback_asNurse_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/feedback/1").with(user(principal(1L, "NURSE"))))
           .andExpect(status().isNoContent());
    }

    @Test void deleteFeedback_asReceptionist_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/feedback/1").with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isNoContent());
    }

    @Test void deleteFeedback_asPatient_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/feedback/1").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isForbidden());
    }

    @Test void deleteFeedback_asDoctor_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/feedback/1").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/inventory  —  any authenticated user
    // ══════════════════════════════════════════════════════════════════════════

    @Test void listInventory_anonymous_is401() throws Exception {
        mvc.perform(get("/api/v1/inventory"))
           .andExpect(status().isUnauthorized());
    }

    @Test void listInventory_asDoctor_isOk() throws Exception {
        mvc.perform(get("/api/v1/inventory").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isOk());
    }

    @Test void listInventory_asPatient_isOk() throws Exception {
        mvc.perform(get("/api/v1/inventory").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/inventory/low-stock  —  ADMIN, NURSE, or RECEPTIONIST
    // ══════════════════════════════════════════════════════════════════════════

    @Test void getLowStock_asAdmin_isOk() throws Exception {
        mvc.perform(get("/api/v1/inventory/low-stock").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isOk());
    }

    @Test void getLowStock_asNurse_isOk() throws Exception {
        mvc.perform(get("/api/v1/inventory/low-stock").with(user(principal(1L, "NURSE"))))
           .andExpect(status().isOk());
    }

    @Test void getLowStock_asReceptionist_isOk() throws Exception {
        mvc.perform(get("/api/v1/inventory/low-stock").with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isOk());
    }

    @Test void getLowStock_asPatient_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/inventory/low-stock").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isForbidden());
    }

    @Test void getLowStock_asDoctor_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/inventory/low-stock").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    @Test void getLowStock_anonymous_is401() throws Exception {
        mvc.perform(get("/api/v1/inventory/low-stock"))
           .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/v1/inventory  —  ADMIN, NURSE, or RECEPTIONIST
    // ══════════════════════════════════════════════════════════════════════════

    private static final String INVENTORY_BODY = """
            {"name":"Aspirin 100mg","sku":"MED-ASP-100","quantityOnHand":500,"reorderLevel":50}
            """;

    @Test void addInventoryItem_asAdmin_isCreated() throws Exception {
        mvc.perform(post("/api/v1/inventory")
                .with(user(principal(1L, "ADMIN")))
                .contentType("application/json")
                .content(INVENTORY_BODY))
           .andExpect(status().isCreated());
    }

    @Test void addInventoryItem_asNurse_isCreated() throws Exception {
        mvc.perform(post("/api/v1/inventory")
                .with(user(principal(1L, "NURSE")))
                .contentType("application/json")
                .content(INVENTORY_BODY))
           .andExpect(status().isCreated());
    }

    @Test void addInventoryItem_asReceptionist_isCreated() throws Exception {
        mvc.perform(post("/api/v1/inventory")
                .with(user(principal(1L, "RECEPTIONIST")))
                .contentType("application/json")
                .content(INVENTORY_BODY))
           .andExpect(status().isCreated());
    }

    @Test void addInventoryItem_asDoctor_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/inventory")
                .with(user(principal(1L, "DOCTOR")))
                .contentType("application/json")
                .content(INVENTORY_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void addInventoryItem_asPatient_isForbidden() throws Exception {
        mvc.perform(post("/api/v1/inventory")
                .with(user(principal(1L, "PATIENT")))
                .contentType("application/json")
                .content(INVENTORY_BODY))
           .andExpect(status().isForbidden());
    }

    @Test void addInventoryItem_anonymous_is401() throws Exception {
        mvc.perform(post("/api/v1/inventory")
                .contentType("application/json")
                .content(INVENTORY_BODY))
           .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/inventory/{id}  —  ADMIN or RECEPTIONIST
    // ══════════════════════════════════════════════════════════════════════════

    @Test void deleteInventoryItem_asAdmin_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/inventory/1").with(user(principal(1L, "ADMIN"))))
           .andExpect(status().isNoContent());
    }

    @Test void deleteInventoryItem_asReceptionist_isNoContent() throws Exception {
        mvc.perform(delete("/api/v1/inventory/1").with(user(principal(1L, "RECEPTIONIST"))))
           .andExpect(status().isNoContent());
    }

    @Test void deleteInventoryItem_asNurse_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/inventory/1").with(user(principal(1L, "NURSE"))))
           .andExpect(status().isForbidden());
    }

    @Test void deleteInventoryItem_asDoctor_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/inventory/1").with(user(principal(1L, "DOCTOR"))))
           .andExpect(status().isForbidden());
    }

    @Test void deleteInventoryItem_asPatient_isForbidden() throws Exception {
        mvc.perform(delete("/api/v1/inventory/1").with(user(principal(1L, "PATIENT"))))
           .andExpect(status().isForbidden());
    }
}
