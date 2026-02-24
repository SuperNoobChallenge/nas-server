package io.github.supernoobchallenge.nasserver.user.controller.api;

import io.github.supernoobchallenge.nasserver.global.dto.api.ErrorResponse;
import io.github.supernoobchallenge.nasserver.share.service.ShareInvitationService;
import io.github.supernoobchallenge.nasserver.user.dto.api.ChangePasswordRequest;
import io.github.supernoobchallenge.nasserver.user.dto.api.InviteRegisterRequest;
import io.github.supernoobchallenge.nasserver.user.dto.api.InviteRegisterResponse;
import io.github.supernoobchallenge.nasserver.user.dto.api.PasswordResetConfirmRequest;
import io.github.supernoobchallenge.nasserver.user.dto.api.PasswordResetRequest;
import io.github.supernoobchallenge.nasserver.user.service.PasswordResetService;
import io.github.supernoobchallenge.nasserver.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ShareInvitationService shareInvitationService;
    private final PasswordResetService passwordResetService;

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long userId, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite-register")
    public ResponseEntity<InviteRegisterResponse> inviteRegister(@RequestBody InviteRegisterRequest request) {
        Long userId = shareInvitationService.registerByInviteLink(
                request.shareUuid(),
                request.linkPassword(),
                request.loginId(),
                request.password(),
                request.email()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new InviteRegisterResponse(userId));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}
