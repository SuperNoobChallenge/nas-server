package io.github.supernoobchallenge.nasserver.file.core.controller;

import io.github.supernoobchallenge.nasserver.file.core.dto.CreateVirtualDirectoryRequest;
import io.github.supernoobchallenge.nasserver.file.core.dto.CreateVirtualDirectoryResponse;
import io.github.supernoobchallenge.nasserver.file.core.dto.MoveVirtualDirectoryRequest;
import io.github.supernoobchallenge.nasserver.file.core.dto.RenameVirtualDirectoryRequest;
import io.github.supernoobchallenge.nasserver.file.core.dto.VirtualDirectoryChildResponse;
import io.github.supernoobchallenge.nasserver.file.core.dto.VirtualDirectoryTreeResponse;
import io.github.supernoobchallenge.nasserver.file.core.service.VirtualDirectoryService;
import io.github.supernoobchallenge.nasserver.global.security.AuditorAwareImpl;
import io.github.supernoobchallenge.nasserver.user.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directories")
@RequiredArgsConstructor
public class VirtualDirectoryController {
    private final VirtualDirectoryService virtualDirectoryService;
    private final AuditorAwareImpl auditorAware;

    @PostMapping
    public ResponseEntity<CreateVirtualDirectoryResponse> createDirectory(@RequestBody CreateVirtualDirectoryRequest request) {
        Long requesterUserId = auditorAware.getAuthenticatedAuditor()
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));

        Long directoryId = virtualDirectoryService.createDirectory(
                requesterUserId,
                request.parentDirectoryId(),
                request.name(),
                request.readLevel(),
                request.writeLevel()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateVirtualDirectoryResponse(directoryId));
    }

    @GetMapping
    public ResponseEntity<List<VirtualDirectoryChildResponse>> listChildren(
            @RequestParam(required = false) Long parentDirectoryId
    ) {
        Long requesterUserId = auditorAware.getAuthenticatedAuditor()
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        return ResponseEntity.ok(virtualDirectoryService.listChildren(requesterUserId, parentDirectoryId));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<VirtualDirectoryTreeResponse>> listDirectoryTree() {
        Long requesterUserId = auditorAware.getAuthenticatedAuditor()
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        return ResponseEntity.ok(virtualDirectoryService.listDirectoryTree(requesterUserId));
    }

    @PatchMapping("/{directoryId}/name")
    public ResponseEntity<Void> renameDirectory(
            @PathVariable Long directoryId,
            @RequestBody RenameVirtualDirectoryRequest request
    ) {
        Long requesterUserId = auditorAware.getAuthenticatedAuditor()
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        virtualDirectoryService.renameDirectory(requesterUserId, directoryId, request.name());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{directoryId}/parent")
    public ResponseEntity<Void> moveDirectory(
            @PathVariable Long directoryId,
            @RequestBody MoveVirtualDirectoryRequest request
    ) {
        Long requesterUserId = auditorAware.getAuthenticatedAuditor()
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        virtualDirectoryService.moveDirectory(requesterUserId, directoryId, request.newParentDirectoryId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{directoryId}")
    public ResponseEntity<Void> requestDeleteDirectory(@PathVariable Long directoryId) {
        Long requesterUserId = auditorAware.getAuthenticatedAuditor()
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        virtualDirectoryService.requestDeleteDirectory(requesterUserId, directoryId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}
