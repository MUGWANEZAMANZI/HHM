package com.controller;

import com.dto.AppointmentRequest;
import com.model.Appointment;
import com.model.AppointmentView;
import com.service.AppointmentService;
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
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointment", description = "Operations for managing appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @Operation(summary = "List all appointments", description = "Returns all appointments. Filter by ?search= or ?status=.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointments returned successfully")
    })
    public ResponseEntity<List<AppointmentView>> getAllAppointments(
            @Parameter(description = "Optional keyword — filters by patient or doctor name")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status: Scheduled, Completed, Cancelled, Rescheduled")
            @RequestParam(required = false) String status) {
        List<AppointmentView> result;
        if (status != null && !status.isBlank())
            result = appointmentService.findByStatus(status);
        else if (search != null && !search.isBlank())
            result = appointmentService.search(search);
        else
            result = appointmentService.getAll();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Book an appointment", description = "Schedules a new appointment. Date must be in the future.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Appointment booked successfully"),
        @ApiResponse(responseCode = "422", description = "Appointment date is in the past"),
        @ApiResponse(responseCode = "422", description = "Validation failed — missing or invalid fields")
    })
    public ResponseEntity<String> bookAppointment(@Valid @RequestBody AppointmentRequest req) {
        try {
            LocalDateTime date = LocalDateTime.parse(req.getAppointmentDate());
            Appointment a = new Appointment(req.getPatientId(), req.getDoctorId(), date,
                    req.getStatus() != null ? req.getStatus() : "Scheduled");
            appointmentService.book(a);
            return ResponseEntity.status(201).body("Appointment booked successfully");
        } catch (ServiceException e) {
            return ResponseEntity.status(422).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(422).body("Invalid date format. Use ISO 8601: 2026-11-15T09:30:00");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reschedule an appointment", description = "Updates date and/or doctor for an existing appointment.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment rescheduled"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "422", description = "New date is in the past")
    })
    public ResponseEntity<String> rescheduleAppointment(
            @Parameter(description = "ID of the appointment to reschedule", example = "1")
            @PathVariable long id,
            @Valid @RequestBody AppointmentRequest req) {
        try {
            LocalDateTime date = LocalDateTime.parse(req.getAppointmentDate());
            Appointment a = new Appointment(req.getPatientId(), req.getDoctorId(), date, "Rescheduled");
            a.setId(id);
            appointmentService.reschedule(a);
            return ResponseEntity.ok("Appointment " + id + " rescheduled");
        } catch (ServiceException e) {
            return switch (e.getType()) {
                case NOT_FOUND -> ResponseEntity.status(404).body(e.getMessage());
                default        -> ResponseEntity.status(422).body(e.getMessage());
            };
        } catch (Exception e) {
            return ResponseEntity.status(422).body("Invalid date format. Use ISO 8601: 2026-11-15T09:30:00");
        }
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment", description = "Sets the appointment status to Cancelled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment cancelled"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "409", description = "Appointment is already cancelled")
    })
    public ResponseEntity<String> cancelAppointment(
            @Parameter(description = "ID of the appointment to cancel", example = "1")
            @PathVariable long id) {
        try {
            appointmentService.cancel(id);
            return ResponseEntity.ok("Appointment " + id + " cancelled");
        } catch (ServiceException e) {
            return switch (e.getType()) {
                case NOT_FOUND -> ResponseEntity.status(404).body(e.getMessage());
                default        -> ResponseEntity.status(409).body(e.getMessage());
            };
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an appointment", description = "Permanently removes an appointment record.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Appointment deleted — no content returned"),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<String> deleteAppointment(
            @Parameter(description = "ID of the appointment to delete", example = "1")
            @PathVariable long id) {
        try {
            appointmentService.remove(id);
            return ResponseEntity.noContent().build();
        } catch (ServiceException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
