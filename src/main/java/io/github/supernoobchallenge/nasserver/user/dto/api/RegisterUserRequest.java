package io.github.supernoobchallenge.nasserver.user.dto.api;

public record RegisterUserRequest(
        String loginId,
        String password,
        String email
) {
}
