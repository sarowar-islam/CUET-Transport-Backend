package com.cuet_transport_backend.config;

import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.repository.UserRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VerificationStatusInitializer implements CommandLineRunner {

    private static final Set<String> VERIFIED_USERNAMES = Set.of(
            "admin",
            "student1",
            "staff1",
            "stuff1",
            "teacher1",
            "teacehr1");

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<User> users = userRepository.findAll();

        boolean requiresInitialization = users.stream().anyMatch(user -> user.getIsVerified() == null);
        if (!requiresInitialization) {
            return;
        }

        for (User user : users) {
            boolean shouldBeVerified = VERIFIED_USERNAMES.contains(user.getUsername().toLowerCase());
            user.setIsVerified(shouldBeVerified);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
        }

        userRepository.saveAll(users);
    }
}
