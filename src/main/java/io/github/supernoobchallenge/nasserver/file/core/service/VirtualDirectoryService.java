package io.github.supernoobchallenge.nasserver.file.core.service;

import io.github.supernoobchallenge.nasserver.batch.service.DirectoryService;
import io.github.supernoobchallenge.nasserver.file.core.dto.VirtualDirectoryChildResponse;
import io.github.supernoobchallenge.nasserver.file.core.dto.VirtualDirectoryTreeResponse;
import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectory;
import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectoryStats;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryStatsRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VirtualDirectoryService {
    private final VirtualDirectoryRepository virtualDirectoryRepository;
    private final VirtualDirectoryStatsRepository virtualDirectoryStatsRepository;
    private final UserRepository userRepository;
    private final DirectoryService directoryService;

    @Transactional
    public Long createDirectory(Long requesterUserId, Long parentDirectoryId, String name, Integer readLevel, Integer writeLevel) {
        String normalizedName = validateName(name);
        int validatedReadLevel = validatePermissionLevel(readLevel, "readLevel");
        int validatedWriteLevel = validatePermissionLevel(writeLevel, "writeLevel");
        if (validatedReadLevel > validatedWriteLevel) {
            throw new IllegalArgumentException("readLevel은 writeLevel보다 클 수 없습니다.");
        }

        User requester = getActiveUser(requesterUserId);
        VirtualDirectory parentDirectory = null;
        if (parentDirectoryId != null) {
            parentDirectory = getAccessibleDirectory(parentDirectoryId, requester);
        }

        validateDuplicateName(
                requester.getFilePermission().getId(),
                parentDirectory == null ? null : parentDirectory.getId(),
                normalizedName,
                null
        );

        VirtualDirectory directory = VirtualDirectory.builder()
                .filePermission(requester.getFilePermission())
                .parentDirectory(parentDirectory)
                .readLevel(validatedReadLevel)
                .writeLevel(validatedWriteLevel)
                .name(normalizedName)
                .build();

        VirtualDirectory saved = virtualDirectoryRepository.save(directory);
        virtualDirectoryStatsRepository.save(VirtualDirectoryStats.init(saved));
        return saved.getId();
    }

    @Transactional
    public void renameDirectory(Long requesterUserId, Long directoryId, String newName) {
        User requester = getActiveUser(requesterUserId);
        VirtualDirectory target = getAccessibleDirectory(directoryId, requester);
        String normalizedName = validateName(newName);

        validateDuplicateName(
                requester.getFilePermission().getId(),
                target.getParentDirectory() == null ? null : target.getParentDirectory().getId(),
                normalizedName,
                target.getId()
        );

        target.rename(normalizedName);
    }

    @Transactional
    public void moveDirectory(Long requesterUserId, Long directoryId, Long newParentDirectoryId) {
        User requester = getActiveUser(requesterUserId);
        VirtualDirectory target = getAccessibleDirectory(directoryId, requester);

        VirtualDirectory newParent = null;
        if (newParentDirectoryId != null) {
            newParent = getAccessibleDirectory(newParentDirectoryId, requester);

            if (target.getId().equals(newParent.getId())) {
                throw new IllegalArgumentException("자기 자신을 부모로 이동할 수 없습니다.");
            }

            List<Long> descendantIds = virtualDirectoryRepository.findAllDescendantIds(List.of(target.getId()));
            if (descendantIds.contains(newParent.getId())) {
                throw new IllegalArgumentException("하위 디렉터리로는 이동할 수 없습니다.");
            }
        }

        validateDuplicateName(
                requester.getFilePermission().getId(),
                newParent == null ? null : newParent.getId(),
                target.getName(),
                target.getId()
        );

        target.moveDirectory(newParent);
    }

    @Transactional
    public void requestDeleteDirectory(Long requesterUserId, Long directoryId) {
        User requester = getActiveUser(requesterUserId);
        getAccessibleDirectory(directoryId, requester);
        directoryService.deleteDirectory(directoryId, requester.getId());
    }

    @Transactional
    public List<VirtualDirectoryChildResponse> listChildren(Long requesterUserId, Long parentDirectoryId) {
        User requester = getActiveUser(requesterUserId);
        if (parentDirectoryId != null) {
            getAccessibleDirectory(parentDirectoryId, requester);
        }

        return virtualDirectoryRepository
                .findActiveChildren(requester.getFilePermission().getId(), parentDirectoryId)
                .stream()
                .map(directory -> new VirtualDirectoryChildResponse(
                        directory.getId(),
                        directory.getParentDirectory() == null ? null : directory.getParentDirectory().getId(),
                        directory.getName(),
                        directory.getReadLevel(),
                        directory.getWriteLevel(),
                        directory.getDepthLevel()
                ))
                .toList();
    }

    @Transactional
    public List<VirtualDirectoryTreeResponse> listDirectoryTree(Long requesterUserId) {
        User requester = getActiveUser(requesterUserId);
        return buildTree(requester.getFilePermission().getId(), null);
    }

    private User getActiveUser(Long requesterUserId) {
        if (requesterUserId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("요청 사용자가 존재하지 않습니다."));
        if (requester.isDeleted()) {
            throw new IllegalArgumentException("비활성화된 사용자입니다.");
        }
        return requester;
    }

    private VirtualDirectory getAccessibleDirectory(Long directoryId, User requester) {
        if (directoryId == null) {
            throw new IllegalArgumentException("directoryId는 필수입니다.");
        }

        VirtualDirectory directory = virtualDirectoryRepository.findActiveById(directoryId)
                .orElseThrow(() -> new IllegalArgumentException("디렉터리를 찾을 수 없습니다."));
        if (!directory.getFilePermission().getId().equals(requester.getFilePermission().getId())) {
            throw new IllegalArgumentException("접근 권한이 없는 디렉터리입니다.");
        }
        return directory;
    }

    private void validateDuplicateName(Long filePermissionId, Long parentDirectoryId, String name, Long excludeDirectoryId) {
        boolean duplicated = virtualDirectoryRepository.existsActiveSiblingName(
                filePermissionId,
                parentDirectoryId,
                name,
                excludeDirectoryId
        );
        if (duplicated) {
            throw new IllegalArgumentException("같은 경로에 동일한 디렉터리 이름이 이미 존재합니다.");
        }
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("디렉터리 이름은 비어있을 수 없습니다.");
        }
        return name.trim();
    }

    private int validatePermissionLevel(Integer level, String fieldName) {
        if (level == null) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        if (level < 0) {
            throw new IllegalArgumentException(fieldName + "은 0 이상이어야 합니다.");
        }
        return level;
    }

    private List<VirtualDirectoryTreeResponse> buildTree(Long filePermissionId, Long parentDirectoryId) {
        return virtualDirectoryRepository.findActiveChildren(filePermissionId, parentDirectoryId)
                .stream()
                .map(directory -> new VirtualDirectoryTreeResponse(
                        directory.getId(),
                        directory.getParentDirectory() == null ? null : directory.getParentDirectory().getId(),
                        directory.getName(),
                        directory.getReadLevel(),
                        directory.getWriteLevel(),
                        directory.getDepthLevel(),
                        buildTree(filePermissionId, directory.getId())
                ))
                .toList();
    }
}
