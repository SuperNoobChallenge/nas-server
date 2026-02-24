package io.github.supernoobchallenge.nasserver.user.dto.api;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
