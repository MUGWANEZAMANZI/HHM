package com.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body for submitting patient feedback")
public class FeedbackRequest {

    @NotNull(message = "Patient ID is required")
    @Min(value = 1, message = "Patient ID must be a positive number")
    @Schema(description = "ID of the patient submitting feedback", example = "1")
    private Long patientId;

    @NotBlank(message = "Feedback text is required")
    @Schema(description = "The patient's feedback or comment", example = "Staff were very professional and caring.")
    private String feedback;

    @Schema(description = "Internal staff notes — not visible to the patient", example = "Follow up required")
    private String privateNotes;
}
