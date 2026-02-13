package io.github.supernoobchallenge.nasserver.share.dto;

import java.time.LocalDateTime;

public record CreateInviteLinkResponse(
        Long shareLinkId,
        String shareUuid,
        String inviteUrl,
        LocalDateTime expirationDate,
        int maxUseCount
) {
}
