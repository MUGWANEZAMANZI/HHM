package com.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body for booking or rescheduling an appointment")
public class AppointmentRequest {

    @NotNull(message = "Patient ID is required")
    @Min(value = 1, message = "Patient ID must be a positive number")
    @Schema(description = "ID of the patient", example = "1")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    @Min(value = 1, message = "Doctor ID must be a positive number")
    @Schema(description = "ID of the doctor", example = "2")
    private Long doctorId;

    @NotBlank(message = "Appointment date is required")
    @Schema(description = "ISO 8601 date-time for the appointment", example = "2026-11-15T09:30:00")
    private String appointmentDate;

    @Schema(description = "Appointment status", example = "Scheduled",
            allowableValues = {"Scheduled", "Completed", "Cancelled", "Rescheduled"})
    private String status = "Scheduled";
}
