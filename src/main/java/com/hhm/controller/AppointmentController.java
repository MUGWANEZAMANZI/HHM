package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.dto.AppointmentRequest;
import com.hhm.model.AppointmentView;
import com.hhm.service.AppointmentService;
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
               description = "Returns paginated appointments. Filter by status with ?status=Scheduled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointments returned successfully")
    })
    public ResponseEntity<ApiResult<Page<AppointmentView>>> getAllAppointments(
            @Parameter(description = "Filter by status: Scheduled | Completed | Cancelled | Rescheduled")
            @RequestParam(required = false) String status,
            @ParameterObject @PageableDefault(size = 20, sort = "appointmentDate") Pageable pageable) {

        List<AppointmentView> source = (status != null && !status.isBlank())
            ? appointmentService.findByStatus(status)
            : appointmentService.getAll();

        int start   = (int) pageable.getOffset();
        int end     = Math.min(start + pageable.getPageSize(), source.size());
        List<AppointmentView> content = start >= source.size() ? List.of() : source.subList(start, end);
        Page<AppointmentView> page = new PageImpl<>(content, pageable, source.size());
        String msg = status != null && !status.isBlank()
            ? source.size() + " appointment(s) with status: " + status
            : String.format("Page %d — %d of %d appointments",
                pageable.getPageNumber(), page.getNumberOfElements(), page.getTotalElements());
        return ResponseEntity.ok(ApiResult.success(msg, page));
    }

    @PreAuthorize("hasAuthority('PATIENT') or hasAuthority('RECEPTIONIST')")
    @PostMapping
    @Operation(summary = "Book an appointment",
               description = "Schedules a new appointment. Date must be ISO 8601 and in the future.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Appointment booked successfully"),
        @ApiResponse(responseCode = "422", description = "Date is in the past or invalid format"),
        @ApiResponse(responseCode = "404", description = "Patient or doctor not found")
    })
    public ResponseEntity<ApiResult<Void>> bookAppointment(@Valid @RequestBody AppointmentRequest req) {
        LocalDateTime date = LocalDateTime.parse(req.getAppointmentDate());
        String status = req.getStatus() != null ? req.getStatus() : "Scheduled";
        appointmentService.book(req.getPatientId(), req.getDoctorId(), date, status);
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
        appointmentService.reschedule(id, req.getPatientId(), req.getDoctorId(), date);
        return ResponseEntity.ok(ApiResult.success("Appointment " + id + " rescheduled", null));
    }


    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment cancelled"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "409", description = "Appointment is already cancelled")
    })

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('RECEPTIONIST')")
    public ResponseEntity<ApiResult<Void>> cancelAppointment(@PathVariable long id) {
        appointmentService.cancel(id);
        return ResponseEntity.ok(ApiResult.success("Appointment " + id + " cancelled", null));
    }


    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an appointment")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Appointment deleted — no content"),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<Void> deleteAppointment(@PathVariable long id) {
        appointmentService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
