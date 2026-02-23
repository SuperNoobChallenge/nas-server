package io.github.supernoobchallenge.nasserver.file.core.service;

import io.github.supernoobchallenge.nasserver.batch.service.DirectoryService;
import io.github.supernoobchallenge.nasserver.file.core.dto.VirtualDirectoryChildResponse;
import io.github.supernoobchallenge.nasserver.file.core.dto.VirtualDirectoryTreeResponse;
import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectory;
import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectoryStats;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryStatsRepository;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirtualDirectoryServiceTest {

    @Mock
    private VirtualDirectoryRepository virtualDirectoryRepository;

    @Mock
    private VirtualDirectoryStatsRepository virtualDirectoryStatsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DirectoryService directoryService;

    @InjectMocks
    private VirtualDirectoryService virtualDirectoryService;

    @Test
    @DisplayName("디렉터리 생성은 저장 후 통계 레코드를 초기화한다")
    void createDirectory_SavesDirectoryAndStats() {
        User requester = newUser(1L, 100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(virtualDirectoryRepository.existsActiveSiblingName(100L, null, "docs", null)).thenReturn(false);
        when(virtualDirectoryRepository.save(any(VirtualDirectory.class))).thenAnswer(invocation -> {
            VirtualDirectory directory = invocation.getArgument(0);
            ReflectionTestUtils.setField(directory, "id", 10L);
            return directory;
        });

        Long directoryId = virtualDirectoryService.createDirectory(1L, null, "docs", 0, 1);

        assertThat(directoryId).isEqualTo(10L);
        ArgumentCaptor<VirtualDirectoryStats> statsCaptor = ArgumentCaptor.forClass(VirtualDirectoryStats.class);
        verify(virtualDirectoryStatsRepository).save(statsCaptor.capture());
        assertThat(statsCaptor.getValue().getVirtualDirectory().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("같은 부모 경로에 동일한 이름이 있으면 생성을 차단한다")
    void createDirectory_WhenDuplicatedName_Throws() {
        User requester = newUser(1L, 100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(virtualDirectoryRepository.existsActiveSiblingName(100L, null, "docs", null)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                virtualDirectoryService.createDirectory(1L, null, "docs", 0, 0)
        );

        verify(virtualDirectoryRepository, never()).save(any(VirtualDirectory.class));
        verify(virtualDirectoryStatsRepository, never()).save(any(VirtualDirectoryStats.class));
    }

    @Test
    @DisplayName("디렉터리를 자신의 하위 디렉터리로 이동할 수 없다")
    void moveDirectory_WhenParentIsDescendant_Throws() {
        User requester = newUser(1L, 100L);
        VirtualDirectory target = newDirectory(10L, requester.getFilePermission(), null, "root");
        VirtualDirectory descendant = newDirectory(20L, requester.getFilePermission(), target, "child");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(virtualDirectoryRepository.findActiveById(10L)).thenReturn(Optional.of(target));
        when(virtualDirectoryRepository.findActiveById(20L)).thenReturn(Optional.of(descendant));
        when(virtualDirectoryRepository.findAllDescendantIds(List.of(10L))).thenReturn(List.of(10L, 20L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                virtualDirectoryService.moveDirectory(1L, 10L, 20L)
        );

        assertThat(ex.getMessage()).isEqualTo("하위 디렉터리로는 이동할 수 없습니다.");
    }

    @Test
    @DisplayName("디렉터리 삭제 요청은 배치 삭제 서비스로 위임한다")
    void requestDeleteDirectory_DelegatesToBatchService() {
        User requester = newUser(1L, 100L);
        VirtualDirectory target = newDirectory(10L, requester.getFilePermission(), null, "to-delete");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(virtualDirectoryRepository.findActiveById(10L)).thenReturn(Optional.of(target));

        virtualDirectoryService.requestDeleteDirectory(1L, 10L);

        verify(directoryService).deleteDirectory(10L, 1L);
    }

    @Test
    @DisplayName("부모 기준 하위 디렉터리 목록을 조회한다")
    void listChildren_ReturnsMappedResponse() {
        User requester = newUser(1L, 100L);
        VirtualDirectory parent = newDirectory(5L, requester.getFilePermission(), null, "parent");
        VirtualDirectory child = newDirectory(6L, requester.getFilePermission(), parent, "child");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(virtualDirectoryRepository.findActiveById(5L)).thenReturn(Optional.of(parent));
        when(virtualDirectoryRepository.findActiveChildren(100L, 5L)).thenReturn(List.of(child));

        List<VirtualDirectoryChildResponse> result = virtualDirectoryService.listChildren(1L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).directoryId()).isEqualTo(6L);
        assertThat(result.get(0).parentDirectoryId()).isEqualTo(5L);
        assertThat(result.get(0).name()).isEqualTo("child");
    }

    @Test
    @DisplayName("디렉터리 트리 조회는 하위 트리를 재귀적으로 반환한다")
    void listDirectoryTree_ReturnsNestedTree() {
        User requester = newUser(1L, 100L);
        VirtualDirectory root = newDirectory(10L, requester.getFilePermission(), null, "root");
        VirtualDirectory child = newDirectory(11L, requester.getFilePermission(), root, "child");

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(virtualDirectoryRepository.findActiveChildren(100L, null)).thenReturn(List.of(root));
        when(virtualDirectoryRepository.findActiveChildren(100L, 10L)).thenReturn(List.of(child));
        when(virtualDirectoryRepository.findActiveChildren(100L, 11L)).thenReturn(List.of());

        List<VirtualDirectoryTreeResponse> tree = virtualDirectoryService.listDirectoryTree(1L);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).directoryId()).isEqualTo(10L);
        assertThat(tree.get(0).children()).hasSize(1);
        assertThat(tree.get(0).children().get(0).directoryId()).isEqualTo(11L);
        assertThat(tree.get(0).children().get(0).children()).isEmpty();
    }

    private User newUser(Long userId, Long permissionId) {
        FilePermissionKey permissionKey = FilePermissionKey.builder()
                .ownerType(USER)
                .build();
        ReflectionTestUtils.setField(permissionKey, "id", permissionId);

        User user = User.builder()
                .loginId("user-" + userId)
                .password("encoded")
                .email("user-" + userId + "@test.com")
                .filePermission(permissionKey)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private VirtualDirectory newDirectory(Long directoryId, FilePermissionKey permissionKey, VirtualDirectory parent, String name) {
        VirtualDirectory directory = VirtualDirectory.builder()
                .filePermission(permissionKey)
                .parentDirectory(parent)
                .readLevel(0)
                .writeLevel(0)
                .name(name)
                .build();
        ReflectionTestUtils.setField(directory, "id", directoryId);
        return directory;
    }
}
