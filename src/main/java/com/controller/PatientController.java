package com.controller;

import com.dto.ApiResult;
import com.dto.PatientRequest;
import com.model.Patient;
import com.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patient", description = "Operations for managing patient records")
public class PatientController {

    private final PatientService patientService;

    PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(
        summary     = "List patients",
        description = "Returns patients with optional search, sorting, and pagination. "
                    + "sort options: name (default), id, email."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patients returned successfully")
    })
    public ResponseEntity<ApiResult<List<Patient>>> getAllPatients(
            @Parameter(description = "Filter by name, email, or phone")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sort field: name (default) | id | email")
            @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "Page number, 0-based")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        List<Patient> all = (search != null && !search.isBlank())
                ? patientService.search(search)
                : patientService.getAll();

        Comparator<Patient> comparator = switch (sort.toLowerCase()) {
            case "id"    -> Comparator.comparingLong(Patient::getId);
            case "email" -> Comparator.comparing(p -> p.getEmail() != null ? p.getEmail() : "");
            default      -> Comparator.comparing(p -> p.getName() != null ? p.getName().toLowerCase() : "");
        };
        all.sort(comparator);

        int total = all.size();
        int from  = page * size;
        List<Patient> paged = from >= total
                ? List.of()
                : all.subList(from, Math.min(from + size, total));

        String msg = String.format("Page %d — showing %d of %d patients", page, paged.size(), total);
        return ResponseEntity.ok(ApiResult.success(msg, paged));
    }

    @PostMapping
    @Operation(
        summary     = "Register a new patient",
        description = "Creates a patient record. Email must be unique."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient registered successfully"),
        @ApiResponse(responseCode = "409", description = "Email is already taken"),
        @ApiResponse(responseCode = "422", description = "Validation failed — see message field")
    })
    public ResponseEntity<ApiResult<Void>> createPatient(@Valid @RequestBody PatientRequest req) {
        long id = patientService.register(
            req.getName(), req.getPhone(), req.getEmail(),
            req.getAddress(), req.getDistrict(), req.getSector(),
            req.getCell(), req.getVillage()
        );
        return ResponseEntity.status(201)
                .body(ApiResult.created("Patient registered with id " + id, null));
    }

    @PutMapping("/{id}")
    @Operation(
        summary     = "Update a patient",
        description = "Updates name, phone, email, address, and location."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient updated successfully"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResult<Void>> updatePatient(
            @Parameter(description = "ID of the patient to update", example = "1")
            @PathVariable long id,
            @Valid @RequestBody PatientRequest req) {
        Patient p = new Patient(req.getName(), req.getPhone(), req.getEmail());
        p.setId(id);
        p.setAddress(req.getAddress());
        patientService.update(p, req.getDistrict(), req.getSector(),
                                 req.getCell(),     req.getVillage());
        return ResponseEntity.ok(ApiResult.success("Patient " + id + " updated", null));
    }

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
