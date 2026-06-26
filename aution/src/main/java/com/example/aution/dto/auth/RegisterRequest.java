package com.example.aution.dto.auth;

import com.example.aution.entity.UserType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RegisterRequest with @Valid constraints.
 * When @Valid is added to the controller parameter, Spring automatically
 * validates these before the method body runs — MethodArgumentNotValidException
 * is thrown and caught by GlobalExceptionHandler if any field fails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone number must be a valid 10-digit Indian mobile number"
    )
    private String phoneNumber;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = "Password must contain uppercase, lowercase, a number, and a special character"
    )
    private String password;

    @NotNull(message = "User type is required (ADMIN, AUCTIONEER, or BIDDER)")
    private UserType userType;

    // Only required when userType = AUCTIONEER
    private String companyName;
}