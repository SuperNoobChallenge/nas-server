package io.github.supernoobchallenge.nasserver.user.dto.api;

public record LoginRequest(
        String loginId,
        String password
) {
}
