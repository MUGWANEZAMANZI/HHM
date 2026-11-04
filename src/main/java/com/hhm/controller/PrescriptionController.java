package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.dto.PrescriptionRequest;
import com.hhm.model.Prescription;
import com.hhm.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/prescriptions")
@Tag(name = "Prescription", description = "Operations for managing prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    @Operation(summary = "List all prescriptions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescriptions returned successfully")
    })
    public ResponseEntity<ApiResult<List<Prescription>>> getAllPrescriptions() {
        return ResponseEntity.ok(
                ApiResult.success("Prescriptions retrieved", prescriptionService.getAll()));
    }

    @PreAuthorize("hasAuthority('DOCTOR') or hasAuthority('NURSE')")
    @PostMapping
    @Operation(summary = "Issue a prescription",
               description = "Creates a new prescription. Patient and doctor must exist.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Prescription issued successfully"),
        @ApiResponse(responseCode = "404", description = "Patient or doctor not found"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResult<Void>> issuePrescription(@Valid @RequestBody PrescriptionRequest req) {
        prescriptionService.issue(req.getPatientId(), req.getDoctorId(),
                                  LocalDateTime.now(), "Active", req.getNotes());
        return ResponseEntity.status(201)
                .body(ApiResult.created("Prescription issued successfully", null));
    }

    @PreAuthorize("hasAuthority('DOCTOR') or hasAuthority('NURSE')")
    @PutMapping("/{id}")
    @Operation(summary = "Update a prescription")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescription updated"),
        @ApiResponse(responseCode = "404", description = "Prescription not found")
    })
    public ResponseEntity<ApiResult<Void>> updatePrescription(
            @Parameter(description = "ID of the prescription to update", example = "1")
            @PathVariable long id,
            @Valid @RequestBody PrescriptionRequest req) {
        prescriptionService.update(id, req.getPatientId(), req.getDoctorId(),
                                   LocalDateTime.now(), "Active", req.getNotes());
        return ResponseEntity.ok(ApiResult.success("Prescription " + id + " updated", null));
    }

    @PreAuthorize("hasAuthority('DOCTOR') or hasAuthority('NURSE')")
    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a prescription")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescription cancelled"),
        @ApiResponse(responseCode = "404", description = "Prescription not found"),
        @ApiResponse(responseCode = "409", description = "Prescription is already cancelled")
    })
    public ResponseEntity<ApiResult<Void>> cancelPrescription(@PathVariable long id) {
        prescriptionService.cancel(id);
        return ResponseEntity.ok(ApiResult.success("Prescription " + id + " cancelled", null));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prescription")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prescription deleted — no content"),
        @ApiResponse(responseCode = "404", description = "Prescription not found")
    })
    public ResponseEntity<Void> deletePrescription(@PathVariable long id) {
        prescriptionService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
