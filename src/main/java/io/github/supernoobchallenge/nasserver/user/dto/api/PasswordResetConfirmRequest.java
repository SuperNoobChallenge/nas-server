package io.github.supernoobchallenge.nasserver.user.dto.api;

public record PasswordResetConfirmRequest(
        String token,
        String newPassword
) {
}
