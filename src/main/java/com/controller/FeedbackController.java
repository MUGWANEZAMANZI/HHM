package com.controller;

import com.dto.ApiResult;
import com.dto.FeedbackRequest;
import com.model.PatientFeedback;
import com.service.PatientFeedbackService;
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
@RequestMapping("/api/v1/feedback")
@Tag(name = "Feedback", description = "Operations for managing patient feedback")
public class FeedbackController {

    private final PatientFeedbackService feedbackService;

    public FeedbackController(PatientFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    @Operation(summary = "List all feedback",
               description = "Returns all patient feedback. Pass ?search= to filter by patient name.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Feedback returned successfully")
    })
    public ResponseEntity<ApiResult<List<PatientFeedback>>> getAllFeedback(
            @Parameter(description = "Filter by patient name")
            @RequestParam(required = false) String search) {
        List<PatientFeedback> result = (search != null && !search.isBlank())
                ? feedbackService.search(search)
                : feedbackService.getAll();
        return ResponseEntity.ok(ApiResult.success("Feedback retrieved", result));
    }

    @PostMapping
    @Operation(summary = "Submit patient feedback",
               description = "Records feedback for a patient. Patient must exist.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Feedback submitted successfully"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "422", description = "Validation failed — feedback text is required")
    })
    public ResponseEntity<ApiResult<Void>> submitFeedback(@Valid @RequestBody FeedbackRequest req) {
        PatientFeedback fb = new PatientFeedback(
                req.getPatientId(), req.getFeedback(),
                req.getPrivateNotes(), LocalDateTime.now());
        feedbackService.submit(fb);
        return ResponseEntity.status(201)
                .body(ApiResult.created("Feedback submitted successfully", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete feedback",
               description = "Permanently removes a feedback record.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Feedback deleted — no content"),
        @ApiResponse(responseCode = "404", description = "Feedback not found")
    })
    public ResponseEntity<Void> deleteFeedback(
            @Parameter(description = "ID of the feedback to delete", example = "1")
            @PathVariable long id) {
        feedbackService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
