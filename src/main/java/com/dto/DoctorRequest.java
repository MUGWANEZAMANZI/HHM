package com.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body for registering or updating a doctor")
public class DoctorRequest {

    @NotBlank(message = "Name is required")
    @Schema(description = "Full name of the doctor", example = "Dr. Nkurunziza Eric")
    private String name;

    @NotNull(message = "Department ID is required")
    @Min(value = 1, message = "Department ID must be a positive number")
    @Schema(description = "ID of the department the doctor belongs to", example = "2")
    private Long departmentId;

    @NotBlank(message = "License number is required")
    @Schema(description = "Unique medical license number", example = "RMD-2024-0042")
    private String licenseNumber;
}
