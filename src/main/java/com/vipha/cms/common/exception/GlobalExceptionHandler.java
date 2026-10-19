package com.vipha.cms.common.exception;

import com.vipha.cms.common.dto.ErrorResponse;
import com.vipha.cms.common.dto.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse<Void>> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse<>(
                        HttpStatus.UNAUTHORIZED.value(),
                        false,
                        ex.getMessage(),
                        Instant.now(),
                        null
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse<Void>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse<>(
                        HttpStatus.FORBIDDEN.value(),
                        false,
                        ex.getMessage(),
                        Instant.now(),
                        null
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse<>(
                        HttpStatus.NOT_FOUND.value(),
                        false,
                        ex.getMessage(),
                        Instant.now(),
                        null
                ));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse<Void>> handleBusinessRule(
            BusinessRuleException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse<>(
                        HttpStatus.BAD_REQUEST.value(),
                        false,
                        ex.getMessage(),
                        Instant.now(),
                        null
                ));
    }

    /** A database constraint caught a duplicate the service check missed, e.g. two concurrent requests. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse<>(
                        HttpStatus.CONFLICT.value(),
                        false,
                        "The request conflicts with existing data",
                        Instant.now(),
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<List<FieldErrorDetail>>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<FieldErrorDetail> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorDetail(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse<>(
                        HttpStatus.BAD_REQUEST.value(),
                        false,
                        "Validation failed",
                        Instant.now(),
                        errors
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<Void>> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse<>(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        false,
                        "An unexpected error occurred",
                        Instant.now(),
                        null
                ));
    }
}