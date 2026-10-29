package com.controller;

import com.dto.ApiResult;
import com.dto.AppointmentRequest;
import com.model.Appointment;
import com.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    @Operation(summary = "List appointments",
               description = "Returns paginated appointments. Filter by status with ?status=Scheduled. "
                           + "Sort examples: ?sort=appointmentDate,desc | ?sort=status,asc")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointments returned successfully")
    })
    public ResponseEntity<ApiResult<Page<Appointment>>> getAllAppointments(
            @Parameter(description = "Filter by status: Scheduled | Completed | Cancelled | Rescheduled")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "appointmentDate") Pageable pageable) {

        if (status != null && !status.isBlank()) {
            List<Appointment> results = appointmentService.findByStatus(status);
            Page<Appointment> page = new PageImpl<>(results, pageable, results.size());
            return ResponseEntity.ok(ApiResult.success(
                results.size() + " appointment(s) with status: " + status, page));
        }

        Page<Appointment> page = appointmentService.getAll(pageable);
        String msg = String.format("Page %d — %d of %d appointments",
            pageable.getPageNumber(), page.getNumberOfElements(), page.getTotalElements());
        return ResponseEntity.ok(ApiResult.success(msg, page));
    }

    @PostMapping
    @Operation(summary = "Book an appointment",
               description = "Schedules a new appointment. Date must be ISO 8601 and in the future.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Appointment booked successfully"),
        @ApiResponse(responseCode = "422", description = "Date is in the past or invalid format"),
        @ApiResponse(responseCode = "404", description = "Patient or doctor not found")
    })
    public ResponseEntity<ApiResult<Void>> bookAppointment(@Valid @RequestBody AppointmentRequest req) {
        // @FutureDateTime already validated format + future — parse is safe here
        LocalDateTime date = LocalDateTime.parse(req.getAppointmentDate());
        Appointment a = new Appointment(req.getPatientId(), req.getDoctorId(), date,
                req.getStatus() != null ? req.getStatus() : "Scheduled");
        appointmentService.book(a);
        return ResponseEntity.status(201)
                .body(ApiResult.created("Appointment booked successfully", null));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reschedule an appointment",
               description = "Updates date and/or doctor for an existing appointment.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment rescheduled"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "422", description = "New date is in the past or invalid")
    })
    public ResponseEntity<ApiResult<Void>> rescheduleAppointment(
            @Parameter(description = "ID of the appointment to reschedule", example = "1")
            @PathVariable long id,
            @Valid @RequestBody AppointmentRequest req) {
        LocalDateTime date = LocalDateTime.parse(req.getAppointmentDate());
        Appointment a = new Appointment(req.getPatientId(), req.getDoctorId(), date, "Rescheduled");
        a.setId(id);
        appointmentService.reschedule(a);
        return ResponseEntity.ok(ApiResult.success("Appointment " + id + " rescheduled", null));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment",
               description = "Sets the appointment status to Cancelled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment cancelled"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "409", description = "Appointment is already cancelled")
    })
    public ResponseEntity<ApiResult<Void>> cancelAppointment(
            @Parameter(description = "ID of the appointment to cancel", example = "1")
            @PathVariable long id) {
        appointmentService.cancel(id);
        return ResponseEntity.ok(ApiResult.success("Appointment " + id + " cancelled", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an appointment",
               description = "Permanently removes an appointment record.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Appointment deleted — no content"),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<Void> deleteAppointment(
            @Parameter(description = "ID of the appointment to delete", example = "1")
            @PathVariable long id) {
        appointmentService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
