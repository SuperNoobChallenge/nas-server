package io.github.supernoobchallenge.nasserver.share.controller.api;

import io.github.supernoobchallenge.nasserver.global.dto.api.ErrorResponse;
import io.github.supernoobchallenge.nasserver.share.dto.api.CreateInviteLinkRequest;
import io.github.supernoobchallenge.nasserver.share.dto.CreateInviteLinkResponse;
import io.github.supernoobchallenge.nasserver.share.service.ShareInvitationService;
import io.github.supernoobchallenge.nasserver.global.security.AuditorAwareImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share-links")
@RequiredArgsConstructor
public class ShareInvitationController {
    private final ShareInvitationService shareInvitationService;
    private final AuditorAwareImpl auditorAware;

    @PostMapping("/invites")
    public ResponseEntity<CreateInviteLinkResponse> createInviteLink(@RequestBody CreateInviteLinkRequest request) {
        Long inviterUserId = auditorAware.getAuthenticatedAuditor()
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));

        CreateInviteLinkResponse response = shareInvitationService.createInviteLink(
                inviterUserId,
                request.name(),
                request.validHours(),
                request.maxUseCount(),
                request.linkPassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}
