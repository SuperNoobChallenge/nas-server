package io.github.supernoobchallenge.nasserver.user.service;

public interface PasswordResetMailService {
    void sendPasswordResetEmail(String email, String resetLink);
}
