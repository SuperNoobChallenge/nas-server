package io.github.supernoobchallenge.nasserver.user.dto;

public record InviteRegisterRequest(
        String shareUuid,
        String linkPassword,
        String loginId,
        String password,
        String email
) {
}
