package io.github.supernoobchallenge.nasserver.share.dto;

public record CreateInviteLinkRequest(
        String name,
        Integer validHours,
        Integer maxUseCount,
        String linkPassword
) {
}
