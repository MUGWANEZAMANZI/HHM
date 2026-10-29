package com.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body for creating or updating a patient")
public class PatientRequest {

    @NotBlank(message = "Name is required")
    @Schema(description = "Full name of the patient", example = "Uwimana Chantal")
    private String name;

    @NotBlank(message = "Phone is required")
    @Schema(description = "Phone number", example = "0780000001")
    private String phone;

    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    @Schema(description = "Unique email address", example = "chantal@hhm.rw")
    private String email;

    @Schema(description = "Street or building address", example = "KG 5 Ave")
    private String address;

    @Schema(description = "Administrative district", example = "Gasabo")
    private String district;

    @Schema(description = "Sector within the district", example = "Remera")
    private String sector;

    @Schema(description = "Cell within the sector", example = "Rukiri")
    private String cell;

    @Schema(description = "Village within the cell", example = "Kabuye")
    private String village;
}
