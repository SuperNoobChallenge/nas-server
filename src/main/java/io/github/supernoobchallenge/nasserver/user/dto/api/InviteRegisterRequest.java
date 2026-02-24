package io.github.supernoobchallenge.nasserver.user.dto.api;

public record InviteRegisterRequest(
        String shareUuid,
        String linkPassword,
        String loginId,
        String password,
        String email
) {
}
