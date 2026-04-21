package com.cuet_transport_backend.dto;

import com.cuet_transport_backend.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record SignupRequest(
            @NotBlank String fullName,
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password,
            UserRole role) {
    }

    public record UserResponse(
            Long id,
            String username,
            String email,
            String fullName,
            UserRole role,
            boolean isVerified) {
    }

    public record AuthPayload(
            UserResponse user,
            String token,
            boolean requiresVerification,
            String verificationIdentifier) {
    }

    public record UpdateProfileRequest(
            @NotBlank String fullName) {
    }

    public record VerificationStatusRequest(
            @NotBlank String identifier) {
    }

    public record VerifyEmailRequest(
            @NotBlank String identifier,
            @NotBlank @Size(min = 6, max = 6) String code) {
    }

    public record ResendVerificationCodeRequest(
            @NotBlank String identifier) {
    }

    public record VerificationStatusResponse(
            String username,
            String email,
            boolean isVerified) {
    }
}
