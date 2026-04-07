package com.cuet_transport_backend.controller;

import com.cuet_transport_backend.dto.ApiResponse;
import com.cuet_transport_backend.dto.AuthDtos.AuthPayload;
import com.cuet_transport_backend.dto.AuthDtos.LoginRequest;
import com.cuet_transport_backend.dto.AuthDtos.SignupRequest;
import com.cuet_transport_backend.dto.AuthDtos.UpdateProfileRequest;
import com.cuet_transport_backend.dto.AuthDtos.UserResponse;
import com.cuet_transport_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthPayload>> signup(@Valid @RequestBody SignupRequest request) {
        AuthPayload payload = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok("Signup successful", payload));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthPayload>> login(@Valid @RequestBody LoginRequest request) {
        AuthPayload payload = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", payload));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> profile() {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched", authService.getCurrentUserProfile()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", authService.updateProfile(request)));
    }
}
