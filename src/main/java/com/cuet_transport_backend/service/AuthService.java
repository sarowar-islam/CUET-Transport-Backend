package com.cuet_transport_backend.service;

import com.cuet_transport_backend.dto.AuthDtos.AuthPayload;
import com.cuet_transport_backend.dto.AuthDtos.LoginRequest;
import com.cuet_transport_backend.dto.AuthDtos.SignupRequest;
import com.cuet_transport_backend.dto.AuthDtos.UpdateProfileRequest;
import com.cuet_transport_backend.dto.AuthDtos.UserResponse;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.UserRole;
import com.cuet_transport_backend.repository.UserRepository;
import com.cuet_transport_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthPayload signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? UserRole.STUDENT : request.role());

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(org.springframework.security.core.userdetails.User
                .withUsername(saved.getUsername())
                .password(saved.getPassword())
                .authorities("ROLE_" + saved.getRole().name())
                .build());

        return new AuthPayload(toUserResponse(saved), token);
    }

    public AuthPayload login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        String token = jwtService.generateToken(org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build());

        return new AuthPayload(toUserResponse(user), token);
    }

    public UserResponse getCurrentUserProfile() {
        User user = getCurrentUserEntity();
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();
        user.setFullName(request.fullName());
        return toUserResponse(userRepository.save(user));
    }

    public User getCurrentUserEntity() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole());
    }
}
