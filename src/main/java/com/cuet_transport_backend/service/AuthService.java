package com.cuet_transport_backend.service;

import com.cuet_transport_backend.dto.AuthDtos.AuthPayload;
import com.cuet_transport_backend.dto.AuthDtos.LoginRequest;
import com.cuet_transport_backend.dto.AuthDtos.ResendVerificationCodeRequest;
import com.cuet_transport_backend.dto.AuthDtos.SignupRequest;
import com.cuet_transport_backend.dto.AuthDtos.UpdateProfileRequest;
import com.cuet_transport_backend.dto.AuthDtos.UserResponse;
import com.cuet_transport_backend.dto.AuthDtos.VerificationStatusRequest;
import com.cuet_transport_backend.dto.AuthDtos.VerificationStatusResponse;
import com.cuet_transport_backend.dto.AuthDtos.VerifyEmailRequest;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.UserRole;
import com.cuet_transport_backend.repository.UserRepository;
import com.cuet_transport_backend.security.JwtService;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;

    @Value("${app.verification.code-expiry-minutes:10}")
    private long verificationCodeExpiryMinutes;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public AuthPayload signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserRole role = request.role() == null ? UserRole.STUDENT : request.role();
        validateEmailForRole(normalizedEmail, role);

        User user = new User();
        user.setFullName(request.fullName());
        user.setUsername(request.username());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setIsVerified(Boolean.FALSE);

        applyNewVerificationCode(user);

        User saved = userRepository.save(user);
        sendVerificationEmail(saved);

        return new AuthPayload(toUserResponse(saved), null, true, saved.getEmail());
    }

    public AuthPayload login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalArgumentException("Email not verified. Please verify your email before signing in.");
        }

        String token = jwtService.generateToken(org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build());

        return new AuthPayload(toUserResponse(user), token, false, null);
    }

    public VerificationStatusResponse getVerificationStatus(VerificationStatusRequest request) {
        User user = resolveUserByIdentifier(request.identifier());
        return new VerificationStatusResponse(user.getUsername(), user.getEmail(), Boolean.TRUE.equals(user.getIsVerified()));
    }

    @Transactional
    public AuthPayload verifyEmail(VerifyEmailRequest request) {
        User user = resolveUserByIdentifier(request.identifier());
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            String token = generateToken(user);
            return new AuthPayload(toUserResponse(user), token, false, null);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (user.getVerificationCode() == null || user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Verification code expired. Please request a new code.");
        }

        if (!user.getVerificationCode().equals(request.code().trim())) {
            throw new IllegalArgumentException("Invalid verification code.");
        }

        user.setIsVerified(Boolean.TRUE);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        User verifiedUser = userRepository.save(user);

        return new AuthPayload(toUserResponse(verifiedUser), generateToken(verifiedUser), false, null);
    }

    @Transactional
    public void resendVerificationCode(ResendVerificationCodeRequest request) {
        User user = resolveUserByIdentifier(request.identifier());
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalArgumentException("User is already verified.");
        }

        applyNewVerificationCode(user);
        User saved = userRepository.save(user);
        sendVerificationEmail(saved);
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
                user.getRole(),
                Boolean.TRUE.equals(user.getIsVerified()));
    }

    private void validateEmailForRole(String email, UserRole role) {
        String normalizedEmail = normalizeEmail(email);
        if (role == UserRole.STUDENT && !normalizedEmail.endsWith("@student.cuet.ac.bd")) {
            throw new IllegalArgumentException("Students must register with @student.cuet.ac.bd email");
        }
        if (role == UserRole.TEACHER && !normalizedEmail.endsWith("@cuet.ac.bd")) {
            throw new IllegalArgumentException("Teachers must register with @cuet.ac.bd email");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateVerificationCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return Integer.toString(code);
    }

    private void applyNewVerificationCode(User user) {
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(OffsetDateTime.now().plusMinutes(verificationCodeExpiryMinutes));
    }

    private void sendVerificationEmail(User user) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalArgumentException("Email service is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("CUET Transport Email Verification Code");
        message.setText("Hello " + user.getFullName() + ",\n\n"
                + "Your verification code is: " + user.getVerificationCode() + "\n"
                + "This code will expire in " + verificationCodeExpiryMinutes + " minutes.\n\n"
                + "If you did not create this account, please ignore this email.\n\n"
                + "CUET Transport Section");

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to send verification email. Please try again.");
        }
    }

    private User resolveUserByIdentifier(String identifier) {
        String normalized = identifier == null ? "" : identifier.trim();
        return userRepository.findByUsername(normalized)
                .or(() -> userRepository.findByEmail(normalized.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String generateToken(User user) {
        return jwtService.generateToken(org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build());
    }
}
