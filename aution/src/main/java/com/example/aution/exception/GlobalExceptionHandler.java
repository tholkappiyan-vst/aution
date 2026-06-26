package com.example.aution.exception;

import com.example.aution.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler intercepts every exception thrown anywhere in the
 * application and converts it into a clean, consistent ErrorResponse JSON.
 *
 * Without this, Spring returns a generic 500 "Internal Server Error" for
 * every unhandled exception — which leaks stack traces and gives the client
 * no actionable information.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody on every method.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Registration Errors ───────────────────────────────────────────────────

    /**
     * Thrown by AuthService when username or email is already taken.
     * HTTP 409 CONFLICT — the resource already exists.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    // ── Login / Auth Errors ───────────────────────────────────────────────────

    /**
     * Thrown by Spring Security's AuthenticationManager when the password
     * is wrong. We catch it here and return 401 with a safe vague message.
     *
     * SECURITY NOTE: never say "wrong password" or "user not found" separately.
     * Both cases return the same message to prevent user enumeration attacks.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Invalid username or password",  // intentionally vague
                request
        );
    }

    /**
     * Thrown by CustomUserDetailsService when the username doesn't exist in DB.
     * Same 401 response as bad password — same vague message (security best practice).
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Invalid username or password",  // same message — don't leak user existence
                request
        );
    }

    /**
     * Thrown when login is attempted on a custom InvalidCredentialsException.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request);
    }

    /**
     * Thrown by Spring Security if the account is locked (future feature).
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(
            LockedException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_LOCKED",
                "Your account has been locked. Please contact support.",
                request
        );
    }

    /**
     * Thrown by Spring Security if the account is disabled (future feature).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex,
            HttpServletRequest request) {

        return build(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED",
                "Your account is disabled. Please contact support.",
                request
        );
    }

    // ── Validation Errors (@Valid on request bodies) ──────────────────────────

    /**
     * Thrown when @Valid fails on a request body field.
     * Returns a map of { fieldName: errorMessage } so the client knows
     * exactly which field failed and why.
     *
     * Example:
     * {
     *   "fieldErrors": {
     *     "password": "Password must be at least 8 characters",
     *     "email": "Must be a valid email address"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message("One or more fields are invalid")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── Illegal Arguments (e.g. unknown UserType) ─────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    // ── Catch-all (prevents raw 500 Internal Server Error) ───────────────────

    /**
     * Safety net for anything not explicitly handled above.
     * Returns 500 but with a clean JSON body instead of a stack trace.
     * In production: log the real exception here, return a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex,
            HttpServletRequest request) {

        // Log the real error internally (replace with your logger)
        ex.printStackTrace();

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                request
        );
    }

    // ── Builder Helper ────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}