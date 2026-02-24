package io.github.supernoobchallenge.nasserver.share.dto.api;

public record CreateInviteLinkRequest(
        String name,
        Integer validHours,
        Integer maxUseCount,
        String linkPassword
) {
}
