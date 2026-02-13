package io.github.supernoobchallenge.nasserver.user.dto;

public record RegisterUserRequest(
        String loginId,
        String password,
        String email
) {
}
