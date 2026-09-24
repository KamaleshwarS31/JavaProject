package com.evoting.exception;

import com.evoting.core.exception.*;
import com.evoting.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralized exception handler.
 * SECURITY: Never exposes stack traces, SQL, internal details, or sensitive data to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Input validation failed", request, errors)
        );
    }

    @ExceptionHandler(ElectionStateException.class)
    public ResponseEntity<ApiErrorResponse> handleElectionState(
            ElectionStateException ex, HttpServletRequest request) {
        log.warn("Invalid election state transition: {} -> {}", ex.getCurrentState(), ex.getTargetState());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            buildError(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", ex.getMessage(), request, null)
        );
    }

    @ExceptionHandler(NullifierDuplicateException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateNullifier(
            NullifierDuplicateException ex, HttpServletRequest request) {
        // Security: Log that a duplicate was detected (do NOT log the nullifier hash)
        log.warn("Duplicate nullifier detected for election: {}", ex.getElectionId());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            buildError(HttpStatus.CONFLICT, "ERR_NULLIFIER_DUPLICATE",
                "A ballot has already been cast for this election with this credential.", request, null)
        );
    }

    @ExceptionHandler(BallotValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleBallotValidation(
            BallotValidationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            buildError(HttpStatus.BAD_REQUEST, "BALLOT_VALIDATION_FAILED",
                "Ballot validation failed.", request, ex.getValidationErrors())
        );
    }

    @ExceptionHandler(CredentialException.class)
    public ResponseEntity<ApiErrorResponse> handleCredential(
            CredentialException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            buildError(HttpStatus.FORBIDDEN, "CREDENTIAL_ERROR", ex.getMessage(), request, null)
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            buildError(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied.", request, null)
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            buildError(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password.", request, null)
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        // Log internally but do NOT expose SQL or constraint names to client
        log.error("Data integrity violation at {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            buildError(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data.", request, null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        // Log internally but NEVER expose stack trace or internal message to client
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.", request, null)
        );
    }

    private ApiErrorResponse buildError(HttpStatus status, String errorCode, String message,
                                         HttpServletRequest request, List<String> details) {
        return ApiErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .errorCode(errorCode)
            .message(message)
            .path(request.getRequestURI())
            .correlationId(UUID.randomUUID().toString())
            .details(details)
            .build();
    }
}
