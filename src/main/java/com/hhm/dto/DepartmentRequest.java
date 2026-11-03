package com.hhm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body for creating a new department")
public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Schema(description = "Name of the department", example = "Cardiology")
    private String name;
}
