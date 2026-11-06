package com.hhm.controller;

import com.hhm.dto.AdmitPatientRequest;
import com.hhm.dto.ApiResult;
import com.hhm.dto.PatientRequest;
import com.hhm.dto.RegisterRequest;
import com.hhm.dto.Type;
import com.hhm.model.Patient;
import com.hhm.service.AuthService;
import com.hhm.service.PatientAdmissionService;
import com.hhm.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patient", description = "Operations for managing patient records")
public class PatientController {

    private final PatientService patientService;
    private final PatientAdmissionService admissionService;
    private final AuthService authService;

    PatientController(
            PatientService patientService,
            PatientAdmissionService admissionService,
            AuthService authService
    ) {
        this.patientService = patientService;
        this.admissionService = admissionService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(
        summary     = "List patients",
        description = "Returns patients with optional search and Spring-managed pagination/sorting. "
                    + "Sort examples: ?sort=name,asc | ?sort=email,desc | ?sort=id,asc"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patients returned successfully")
    })
    public ResponseEntity<ApiResult<Page<Patient>>> getAllPatients(
            @Parameter(description = "Filter by name, email, or phone")
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        if (search != null && !search.isBlank()) {
            List<Patient> results = patientService.search(search);
            Page<Patient> page = new PageImpl<>(results, pageable, results.size());
            return ResponseEntity.ok(ApiResult.success(results.size() + " patient(s) found", page));
        }

        Page<Patient> page = patientService.getAll(pageable);
        String msg = String.format("Page %d — %d of %d patients",
            pageable.getPageNumber(), page.getNumberOfElements(), page.getTotalElements());
        return ResponseEntity.ok(ApiResult.success(msg, page));
    }

    @PostMapping("/admit")
    @Operation(
        summary     = "Admit a patient (transactional demo)",
        description = "Registers the patient AND books their first appointment in one atomic transaction. "
                    + "If the doctor does not exist or the date is in the past, "
                    + "the patient record is rolled back too — neither row is saved."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient admitted and appointment booked"),
        @ApiResponse(responseCode = "404", description = "Doctor not found — transaction rolled back"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResult<Void>> admitPatient(@Valid @RequestBody AdmitPatientRequest req) {
        long id = admissionService.admitPatient(
            req.getName(), req.getPhone(), req.getEmail(),
            req.getDoctorId(), req.getAppointmentDate());
        return ResponseEntity.status(201)
                .body(ApiResult.created("Patient admitted with id " + id + " — appointment booked", null));
    }

    @PostMapping
    @Operation(
        summary     = "Register a new patient",
        description = "Creates a patient account via UserFactory. Requires email, password, name, phone."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient registered successfully"),
        @ApiResponse(responseCode = "409", description = "Email already in use"),
        @ApiResponse(responseCode = "422", description = "Validation failed — see message field")
    })
    public ResponseEntity<ApiResult<Void>> createPatient(@Valid @RequestBody RegisterRequest req) {
        req.setType(Type.PATIENT);
        authService.createUser(req);
        return ResponseEntity.status(201)
                .body(ApiResult.created("Patient registered successfully", null));
    }

    // ADMIN can update any patient. A PATIENT can only update their own account.
    @PreAuthorize("hasAuthority('ADMIN') or (hasAuthority('PATIENT') and authentication.principal.id == #id)")
    @PutMapping("/{id}")
    @Operation(
        summary     = "Update a patient",
        description = "Updates name, phone, email, address, and location. Patients can only update their own record."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient updated successfully"),
        @ApiResponse(responseCode = "403", description = "Not allowed to update another patient's account"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResult<Void>> updatePatient(
            @Parameter(description = "ID of the patient to update", example = "1")
            @PathVariable long id,
            @Valid @RequestBody PatientRequest req) {
        Patient p = new Patient(req.getName(), req.getPhone());
        p.setEmail(req.getEmail());
        p.setId(id);
        p.setAddress(req.getAddress());
        patientService.update(p, req.getDistrict(), req.getSector(),
                                 req.getCell(),     req.getVillage());
        return ResponseEntity.ok(ApiResult.success("Patient " + id + " updated", null));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
        summary     = "Delete a patient",
        description = "Removes the patient and their address record permanently."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Patient deleted — no content"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    public ResponseEntity<Void> deletePatient(
            @Parameter(description = "ID of the patient to delete", example = "1")
            @PathVariable long id) {
        patientService.remove(id);
        return ResponseEntity.noContent().build();
    }

}
