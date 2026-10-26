package com.controller;

import com.dto.PatientRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.model.Patient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.service.PatientService;
import com.service.ServiceException;

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
        summary     = "List all patients",
        description = "Returns all patients. Pass ?search= to filter by name, email, or phone."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patients returned successfully")
    })
    public ResponseEntity<List<Patient>> getAllPatients(
            @Parameter(description = "Optional keyword — filters by name, email, or phone")
            @RequestParam(required = false) String search) {
        List<Patient> result = (search != null && !search.isBlank())
                ? patientService.search(search)
                : patientService.getAll();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(
        summary     = "Register a new patient",
        description = "Creates a patient record. Email must be unique across all patients."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient registered successfully"),
        @ApiResponse(responseCode = "409", description = "Email is already taken")
    })
    public ResponseEntity<String> createPatient(@Valid @RequestBody PatientRequest req) {
        try {
            long id = patientService.register(
                req.getName(), req.getPhone(), req.getEmail(),
                req.getAddress(), req.getDistrict(), req.getSector(),
                req.getCell(), req.getVillage()
            );
            return ResponseEntity.status(201).body("Patient registered with id " + id);
        } catch (ServiceException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary     = "Update a patient",
        description = "Updates name, phone, email, address and location for the given patient id."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient updated successfully"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    public ResponseEntity<String> updatePatient(
            @Parameter(description = "ID of the patient to update", example = "1")
            @PathVariable long id,
            @Valid @RequestBody PatientRequest req) {
        try {
            Patient p = new Patient(req.getName(), req.getPhone(), req.getEmail());
            p.setId(id);
            p.setAddress(req.getAddress());
            patientService.update(p, req.getDistrict(), req.getSector(),
                                     req.getCell(),     req.getVillage());
            return ResponseEntity.ok("Patient " + id + " updated");
        } catch (ServiceException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary     = "Delete a patient",
        description = "Removes the patient and their address record permanently."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Patient deleted — no content returned"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    public ResponseEntity<String> deletePatient(
            @Parameter(description = "ID of the patient to delete", example = "1")
            @PathVariable long id) {
        try {
            patientService.remove(id);
            return ResponseEntity.noContent().build();
        } catch (ServiceException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
