package com.hhm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body for issuing or updating a prescription")
public class PrescriptionRequest {

    @NotNull(message = "Patient ID is required")
    @Min(value = 1, message = "Patient ID must be a positive number")
    @Schema(description = "ID of the patient receiving the prescription", example = "1")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    @Min(value = 1, message = "Doctor ID must be a positive number")
    @Schema(description = "ID of the doctor issuing the prescription", example = "2")
    private Long doctorId;

    @Schema(description = "Clinical notes attached to the prescription", example = "Take after meals")
    private String notes;
}
