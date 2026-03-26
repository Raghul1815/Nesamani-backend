package com.nesamani.controller;

import com.nesamani.dto.Dto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Catches every unhandled exception across all controllers.
 * Returns clean JSON so the frontend never receives an HTML error page.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Validation, "not found", business logic errors */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Dto.ErrorResponse> handleRuntime(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Dto.ErrorResponse(e.getMessage(), 400));
    }

    /** 403 — wrong role accessing a protected route */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Dto.ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new Dto.ErrorResponse(
                    "You do not have permission to access this resource.", 403));
    }

    /** Catch-all */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Dto.ErrorResponse> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Dto.ErrorResponse(
                    "An unexpected error occurred. Please try again.", 500));
    }
}
