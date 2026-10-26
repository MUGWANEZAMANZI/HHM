package com.controller;

import com.dto.PrescriptionRequest;
import com.model.Prescription;
import com.service.PrescriptionService;
import com.service.ServiceException;
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
    @Operation(summary = "List all prescriptions", description = "Returns all prescriptions in the system.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescriptions returned successfully")
    })
    public ResponseEntity<List<Prescription>> getAllPrescriptions() {
        return ResponseEntity.ok(prescriptionService.getAll());
    }

    @PostMapping
    @Operation(summary = "Issue a prescription", description = "Creates a new prescription. Patient must exist.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Prescription issued successfully"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    public ResponseEntity<String> issuePrescription(@Valid @RequestBody PrescriptionRequest req) {
        try {
            Prescription p = new Prescription(
                    req.getPatientId(), req.getDoctorId(),
                    LocalDateTime.now(), "Active", req.getNotes());
            prescriptionService.issue(p);
            return ResponseEntity.status(201).body("Prescription issued successfully");
        } catch (ServiceException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a prescription", description = "Updates notes and status for the given prescription.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescription updated"),
        @ApiResponse(responseCode = "404", description = "Prescription not found")
    })
    public ResponseEntity<String> updatePrescription(
            @Parameter(description = "ID of the prescription to update", example = "1")
            @PathVariable long id,
            @Valid @RequestBody PrescriptionRequest req) {
        try {
            Prescription p = new Prescription(
                    req.getPatientId(), req.getDoctorId(),
                    LocalDateTime.now(), "Active", req.getNotes());
            p.setId(id);
            prescriptionService.update(p);
            return ResponseEntity.ok("Prescription " + id + " updated");
        } catch (ServiceException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a prescription", description = "Sets prescription status to Cancelled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prescription cancelled"),
        @ApiResponse(responseCode = "404", description = "Prescription not found"),
        @ApiResponse(responseCode = "409", description = "Prescription is already cancelled")
    })
    public ResponseEntity<String> cancelPrescription(
            @Parameter(description = "ID of the prescription to cancel", example = "1")
            @PathVariable long id) {
        try {
            prescriptionService.cancel(id);
            return ResponseEntity.ok("Prescription " + id + " cancelled");
        } catch (ServiceException e) {
            return switch (e.getType()) {
                case NOT_FOUND -> ResponseEntity.status(404).body(e.getMessage());
                default        -> ResponseEntity.status(409).body(e.getMessage());
            };
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prescription", description = "Permanently removes a prescription record.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prescription deleted — no content returned"),
        @ApiResponse(responseCode = "404", description = "Prescription not found")
    })
    public ResponseEntity<String> deletePrescription(
            @Parameter(description = "ID of the prescription to delete", example = "1")
            @PathVariable long id) {
        try {
            prescriptionService.remove(id);
            return ResponseEntity.noContent().build();
        } catch (ServiceException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
