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
    @Operation(summary = "List all prescriptions",
               description = "Returns all prescriptions in the system.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescriptions returned successfully")
    })
    public ResponseEntity<ApiResult<List<Prescription>>> getAllPrescriptions() {
        return ResponseEntity.ok(
                ApiResult.success("Prescriptions retrieved", prescriptionService.getAll()));
    }

    @PostMapping
    @Operation(summary = "Issue a prescription",
               description = "Creates a new prescription. Patient must exist.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Prescription issued successfully"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResult<Void>> issuePrescription(@Valid @RequestBody PrescriptionRequest req) {
        Prescription p = new Prescription(
                req.getPatientId(), req.getDoctorId(),
                LocalDateTime.now(), "Active", req.getNotes());
        prescriptionService.issue(p);
        return ResponseEntity.status(201)
                .body(ApiResult.created("Prescription issued successfully", null));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a prescription",
               description = "Updates notes and status for the given prescription.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescription updated"),
        @ApiResponse(responseCode = "404", description = "Prescription not found")
    })
    public ResponseEntity<ApiResult<Void>> updatePrescription(
            @Parameter(description = "ID of the prescription to update", example = "1")
            @PathVariable long id,
            @Valid @RequestBody PrescriptionRequest req) {
        Prescription p = new Prescription(
                req.getPatientId(), req.getDoctorId(),
                LocalDateTime.now(), "Active", req.getNotes());
        p.setId(id);
        prescriptionService.update(p);
        return ResponseEntity.ok(ApiResult.success("Prescription " + id + " updated", null));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a prescription",
               description = "Sets prescription status to Cancelled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescription cancelled"),
        @ApiResponse(responseCode = "404", description = "Prescription not found"),
        @ApiResponse(responseCode = "409", description = "Prescription is already cancelled")
    })
    public ResponseEntity<ApiResult<Void>> cancelPrescription(
            @Parameter(description = "ID of the prescription to cancel", example = "1")
            @PathVariable long id) {
        prescriptionService.cancel(id);
        return ResponseEntity.ok(ApiResult.success("Prescription " + id + " cancelled", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prescription",
               description = "Permanently removes a prescription record.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prescription deleted — no content"),
        @ApiResponse(responseCode = "404", description = "Prescription not found")
    })
    public ResponseEntity<Void> deletePrescription(
            @Parameter(description = "ID of the prescription to delete", example = "1")
            @PathVariable long id) {
        prescriptionService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
