package io.github.supernoobchallenge.nasserver.user.dto;

public record LoginResponse(
        Long userId,
        String loginId,
        String email
) {
}
