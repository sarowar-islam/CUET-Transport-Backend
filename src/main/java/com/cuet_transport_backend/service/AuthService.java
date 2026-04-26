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
import java.time.Year;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        String identifier = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password().trim();

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            applyNewVerificationCode(user);
            User saved = userRepository.save(user);
            sendVerificationEmail(saved);
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
        return new VerificationStatusResponse(user.getUsername(), user.getEmail(),
                Boolean.TRUE.equals(user.getIsVerified()));
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
        sendWelcomeEmailBestEffort(verifiedUser);

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

        String subject = "CUET Transport: Verify Your Email";
        String recipientName = escapeHtml(user.getFullName());
        String code = escapeHtml(user.getVerificationCode());
        String htmlBody = """
                <div style=\"font-family:Arial,Helvetica,sans-serif;background-color:#f6f8fb;padding:24px;color:#1f2937;\">
                    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%%\" style=\"max-width:620px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">
                        <tr>
                            <td style=\"background:#0f4c81;padding:20px 24px;color:#ffffff;\">
                                <h2 style=\"margin:0;font-size:20px;\">CUET Transport Section</h2>
                                <p style=\"margin:8px 0 0 0;font-size:14px;opacity:0.95;\">Email Verification Required</p>
                            </td>
                        </tr>
                        <tr>
                            <td style=\"padding:24px;\">
                                <p style=\"margin:0 0 12px 0;font-size:15px;\">Hello <strong>%s</strong>,</p>
                                <p style=\"margin:0 0 16px 0;font-size:15px;line-height:1.6;\">Use the verification code below to complete your account verification.</p>
                                <div style=\"margin:20px 0;padding:16px;background:#f0f9ff;border:1px solid #bae6fd;border-radius:10px;text-align:center;\">
                                    <div style=\"font-size:12px;letter-spacing:0.08em;color:#0369a1;text-transform:uppercase;margin-bottom:8px;\">Verification Code</div>
                                    <div style=\"font-size:32px;letter-spacing:0.22em;font-weight:700;color:#0c4a6e;\">%s</div>
                                </div>
                                <p style=\"margin:0 0 12px 0;font-size:14px;color:#374151;\">This code will expire in <strong>%d minutes</strong>.</p>
                                <p style=\"margin:0;font-size:13px;color:#6b7280;\">If you did not create this account, please ignore this email.</p>
                            </td>
                        </tr>
                        <tr>
                            <td style=\"padding:16px 24px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;color:#6b7280;\">
                                CUET Transport Section • Chittagong University of Engineering and Technology
                            </td>
                        </tr>
                    </table>
                </div>
                """
                .formatted(recipientName, code, verificationCodeExpiryMinutes);

        try {
            sendHtmlEmail(user.getEmail(), subject, htmlBody);
        } catch (MailException ex) {
            throw new IllegalArgumentException("Failed to send verification email. Please try again.");
        }
    }

    private void sendWelcomeEmailBestEffort(User user) {
        if (fromEmail == null || fromEmail.isBlank()) {
            return;
        }

        String recipientName = escapeHtml(user.getFullName());
        String role = escapeHtml(user.getRole().name().toLowerCase());
        String subject = "Welcome to CUET Transport Section";
        String htmlBody = """
                <div style=\"font-family:Arial,Helvetica,sans-serif;background-color:#f6f8fb;padding:24px;color:#1f2937;\">
                    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%%\" style=\"max-width:620px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">
                        <tr>
                            <td style=\"background:#14532d;padding:20px 24px;color:#ffffff;\">
                                <h2 style=\"margin:0;font-size:20px;\">Welcome to CUET Transport</h2>
                                <p style=\"margin:8px 0 0 0;font-size:14px;opacity:0.95;\">Your email is now verified</p>
                            </td>
                        </tr>
                        <tr>
                            <td style=\"padding:24px;\">
                                <p style=\"margin:0 0 12px 0;font-size:15px;\">Hello <strong>%s</strong>,</p>
                                <p style=\"margin:0 0 12px 0;font-size:15px;line-height:1.6;\">Your account has been successfully verified. You can now sign in and use all available transport services.</p>
                                <p style=\"margin:0 0 12px 0;font-size:14px;color:#374151;\">Registered role: <strong>%s</strong></p>
                                <p style=\"margin:0;font-size:14px;color:#374151;\">We are glad to have you with us.</p>
                            </td>
                        </tr>
                        <tr>
                            <td style=\"padding:16px 24px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;color:#6b7280;\">
                                © %d CUET Transport Section
                            </td>
                        </tr>
                    </table>
                </div>
                """
                .formatted(recipientName, role, Year.now().getValue());

        try {
            sendHtmlEmail(user.getEmail(), subject, htmlBody);
        } catch (Exception ex) {
            System.err.println("Failed to send welcome email to " + user.getEmail() + ": " + ex.getMessage());
        }
    }

    private void sendHtmlEmail(String recipient, String subject, String htmlBody) {
        var mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to send email.", ex);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
