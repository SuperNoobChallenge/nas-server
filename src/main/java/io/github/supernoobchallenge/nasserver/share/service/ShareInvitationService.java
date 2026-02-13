package io.github.supernoobchallenge.nasserver.share.service;

import io.github.supernoobchallenge.nasserver.share.dto.CreateInviteLinkResponse;
import io.github.supernoobchallenge.nasserver.share.entity.ShareLink;
import io.github.supernoobchallenge.nasserver.share.repository.ShareLinkRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import io.github.supernoobchallenge.nasserver.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareInvitationService {
    private final ShareLinkRepository shareLinkRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreateInviteLinkResponse createInviteLink(Long inviterUserId, String name, Integer validHours, Integer maxUseCount,
                                                     String rawLinkPassword) {
        if (inviterUserId == null) {
            throw new IllegalArgumentException("inviterUserId는 필수입니다.");
        }
        if (validHours == null || validHours < 1) {
            throw new IllegalArgumentException("validHours는 1 이상이어야 합니다.");
        }
        if (maxUseCount == null || maxUseCount < 1) {
            throw new IllegalArgumentException("maxUseCount는 1 이상이어야 합니다.");
        }

        User inviter = userRepository.findById(inviterUserId)
                .orElseThrow(() -> new IllegalArgumentException("inviter가 존재하지 않습니다."));
        if (inviter.isDeleted()) {
            throw new IllegalArgumentException("비활성화된 inviter입니다.");
        }

        LocalDateTime expirationDate = LocalDateTime.now().plusHours(validHours);
        String shareUuid = UUID.randomUUID().toString().replace("-", "");
        String encodedLinkPassword = (rawLinkPassword == null || rawLinkPassword.isBlank())
                ? null
                : passwordEncoder.encode(rawLinkPassword);

        ShareLink saved = shareLinkRepository.save(
                ShareLink.createInviteLink(inviter, shareUuid, expirationDate, encodedLinkPassword, maxUseCount, name)
        );

        return new CreateInviteLinkResponse(
                saved.getId(),
                saved.getShareUuid(),
                "/api/users/invite-register?shareUuid=" + saved.getShareUuid(),
                saved.getExpirationDate(),
                saved.getMaxUseCount()
        );
    }

    @Transactional
    public Long registerByInviteLink(String shareUuid, String linkPassword, String loginId, String rawPassword, String email) {
        if (shareUuid == null || shareUuid.isBlank()) {
            throw new IllegalArgumentException("shareUuid는 비어있을 수 없습니다.");
        }

        ShareLink shareLink = shareLinkRepository.findByShareUuid(shareUuid)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 링크입니다."));

        validateInviteLinkUsable(shareLink, linkPassword);

        Long invitedUserId = userService.registerInvitedUser(loginId, rawPassword, email, shareLink.getUser().getId());
        shareLink.increaseUseCount();

        return invitedUserId;
    }

    private void validateInviteLinkUsable(ShareLink shareLink, String linkPassword) {
        if (shareLink.isDeleted()) {
            throw new IllegalArgumentException("사용할 수 없는 초대 링크입니다.");
        }
        if (!shareLink.isInviteLink()) {
            throw new IllegalArgumentException("초대용 링크가 아닙니다.");
        }
        if (shareLink.isExpired(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 초대 링크입니다.");
        }
        if (shareLink.isExhausted()) {
            throw new IllegalArgumentException("사용 가능 횟수를 초과한 초대 링크입니다.");
        }
        if (shareLink.hasPassword()) {
            if (linkPassword == null || linkPassword.isBlank()) {
                throw new IllegalArgumentException("링크 비밀번호가 필요합니다.");
            }
            if (!passwordEncoder.matches(linkPassword, shareLink.getPassword())) {
                throw new IllegalArgumentException("링크 비밀번호가 올바르지 않습니다.");
            }
        }
    }
}
