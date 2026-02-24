package io.github.supernoobchallenge.nasserver.file.core.controller;

import io.github.supernoobchallenge.nasserver.file.core.service.VirtualDirectoryService;
import io.github.supernoobchallenge.nasserver.global.security.AuditorAwareImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class VirtualDirectoryPageController {
    private final VirtualDirectoryService virtualDirectoryService;
    private final AuditorAwareImpl auditorAware;

    @GetMapping("/web/directories")
    public String directoryPage(Model model) {
        Long requesterUserId = getRequesterUserId();
        model.addAttribute("directoryTree", virtualDirectoryService.listDirectoryTree(requesterUserId));
        model.addAttribute("requesterUserId", requesterUserId);
        return "web/directories";
    }

    @PostMapping("/web/directories/create")
    public String createDirectory(
            @RequestParam(required = false) Long parentDirectoryId,
            @RequestParam String name,
            @RequestParam Integer readLevel,
            @RequestParam Integer writeLevel,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Long directoryId = virtualDirectoryService.createDirectory(
                    getRequesterUserId(),
                    parentDirectoryId,
                    name,
                    readLevel,
                    writeLevel
            );
            redirectAttributes.addFlashAttribute("successMessage", "디렉터리를 생성했습니다. id=" + directoryId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/web/directories";
    }

    @PostMapping("/web/directories/{directoryId}/rename")
    public String renameDirectory(
            @PathVariable Long directoryId,
            @RequestParam String name,
            RedirectAttributes redirectAttributes
    ) {
        try {
            virtualDirectoryService.renameDirectory(getRequesterUserId(), directoryId, name);
            redirectAttributes.addFlashAttribute("successMessage", "디렉터리 이름을 변경했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/web/directories";
    }

    @PostMapping("/web/directories/{directoryId}/move")
    public String moveDirectory(
            @PathVariable Long directoryId,
            @RequestParam(required = false) Long newParentDirectoryId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            virtualDirectoryService.moveDirectory(getRequesterUserId(), directoryId, newParentDirectoryId);
            redirectAttributes.addFlashAttribute("successMessage", "디렉터리를 이동했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/web/directories";
    }

    @PostMapping("/web/directories/{directoryId}/delete")
    public String deleteDirectory(@PathVariable Long directoryId, RedirectAttributes redirectAttributes) {
        try {
            virtualDirectoryService.requestDeleteDirectory(getRequesterUserId(), directoryId);
            redirectAttributes.addFlashAttribute("successMessage", "디렉터리 삭제를 요청했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/web/directories";
    }

    private Long getRequesterUserId() {
        return auditorAware.getAuthenticatedAuditor()
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
    }
}
