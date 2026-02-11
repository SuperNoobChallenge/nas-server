package io.github.supernoobchallenge.nasserver.user.dto;

public record LoginRequest(
        String loginId,
        String password
) {
}
