package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.service.ServiceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<?>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(ApiResult.error(403, "Access denied"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResult<?>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(401).body(ApiResult.error(401, "Invalid email or password"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<?>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(400).body(ApiResult.error(400, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<?>> handleGeneral(Exception e) {
        return ResponseEntity.status(500).body(ApiResult.error(500, "Unexpected error: " + e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMsg = (ex.getRootCause() != null && ex.getRootCause().getMessage() != null)
                ? ex.getRootCause().getMessage() : "";

        String message = "A database conflict occurred. Please check your input and try again.";

        if (rootMsg.contains("uk_user_email")) {
            message = "Email address is already registered.";
        } else if (rootMsg.contains("uk_doctor_license")) {
            message = "License number is already in use.";
        } else if (rootMsg.contains("uk_patient_phone")) {
            message = "Phone number is already registered.";
        } else if (rootMsg.contains("uk_inventory_sku")) {
            message = "An item with this SKU already exists.";
        } else if (rootMsg.contains("uk_department_name")) {
            message = "A department with this name already exists.";
        } else if (rootMsg.contains("Duplicate entry")) {
            message = "A record with this information already exists.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.error(409, message));
    }
}
