package io.github.supernoobchallenge.nasserver.user.dto;

public record PasswordResetConfirmRequest(
        String token,
        String newPassword
) {
}
