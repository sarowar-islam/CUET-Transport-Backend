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
            UserRole role) {
    }

    public record AuthPayload(
            UserResponse user,
            String token) {
    }

    public record UpdateProfileRequest(
            @NotBlank String fullName) {
    }
}
