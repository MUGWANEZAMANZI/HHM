package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.service.ServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralized error handler — replaces per-controller try/catch blocks.
 *
 * ServiceException  → 404 / 409 / 422 based on its Type enum
 * Validation error  → 422 with field-level messages joined by "; "
 * Anything else     → 500 (should not reach production users in plain text)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResult<?>> handleService(ServiceException e) {
        int status = switch (e.getType()) {
            case NOT_FOUND  -> 404;
            case CONFLICT   -> 409;
            case VALIDATION -> 422;
        };
        return ResponseEntity.status(status).body(ApiResult.error(status, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<?>> handleValidation(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(422).body(ApiResult.error(422, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<?>> handleGeneral(Exception e) {
        return ResponseEntity.status(500).body(ApiResult.error(500, "Unexpected error: " + e.getMessage()));
    }
}
