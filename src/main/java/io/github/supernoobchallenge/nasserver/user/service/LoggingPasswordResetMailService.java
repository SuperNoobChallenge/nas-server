package io.github.supernoobchallenge.nasserver.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingPasswordResetMailService implements PasswordResetMailService {
    @Override
    public void sendPasswordResetEmail(String email, String resetLink) {
        log.info("password-reset mail queued: email={}, link={}", email, resetLink);
    }
}
