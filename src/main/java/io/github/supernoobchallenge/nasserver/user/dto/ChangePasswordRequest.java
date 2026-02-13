package io.github.supernoobchallenge.nasserver.user.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
