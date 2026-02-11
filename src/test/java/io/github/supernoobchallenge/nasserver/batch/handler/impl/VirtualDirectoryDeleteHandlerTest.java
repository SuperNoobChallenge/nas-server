package io.github.supernoobchallenge.nasserver.batch.handler.impl;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.file.core.repository.RealFileRepository;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirtualDirectoryDeleteHandlerTest {

    @Mock
    private VirtualDirectoryRepository directoryRepository;

    @Mock
    private VirtualFileRepository virtualFileRepository;

    @Mock
    private RealFileRepository realFileRepository;

    @InjectMocks
    private VirtualDirectoryDeleteHandler handler;

    @Test
    @DisplayName("jobType은 DIRECTORY_DELETE를 반환한다")
    void getJobType_ReturnsDirectoryDelete() {
        // Handler 라우팅 키가 기대값과 일치하는지 검증
        assertEquals("DIRECTORY_DELETE", handler.getJobType());
    }

    @Test
    @DisplayName("jobs가 비어 있으면 어떤 리포지토리도 호출하지 않는다")
    void handle_WhenJobsEmpty_DoesNothing() {
        // 입력이 비어 있으면 외부 의존성을 호출하지 않고 종료해야 함
        handler.handle(List.of());
        verifyNoInteractions(directoryRepository, virtualFileRepository, realFileRepository);
    }

    @Test
    @DisplayName("target_table이 virtual_directories가 아니면 예외를 던진다")
    void handle_WhenTargetTableInvalid_Throws() {
        // directory 삭제 핸들러는 virtual_directories만 허용해야 함
        BatchJobQueue invalidJob = newJob("virtual_files", 10L);

        assertThrows(IllegalArgumentException.class, () -> handler.handle(List.of(invalidJob)));
        verifyNoInteractions(directoryRepository, virtualFileRepository, realFileRepository);
    }

    @Test
    @DisplayName("정상 jobs면 하위 파일/디렉터리 비활성화와 real_file 참조 카운트 감소를 수행한다")
    void handle_WhenValidJobs_DeletesFilesAndDirectoriesAndDecrementsReferences() {
        // 정상 케이스:
        // 1) 하위 디렉터리 전체 조회
        // 2) 하위 파일 기준 real_file 감소량 집계
        // 3) 파일 soft delete
        // 4) real_file reference_count 감소
        // 5) 디렉터리 soft delete
        BatchJobQueue root1 = newJob("virtual_directories", 1L);
        BatchJobQueue root2 = newJob("virtual_directories", 2L);
        when(directoryRepository.findAllDescendantIds(List.of(1L, 2L))).thenReturn(List.of(1L, 2L, 3L));
        when(virtualFileRepository.countActiveByDirectoryIds(anyList())).thenReturn(List.of(
                new Object[]{50L, 3L},
                new Object[]{60L, 1L}
        ));

        handler.handle(List.of(root1, root2));

        InOrder inOrder = inOrder(directoryRepository, virtualFileRepository, realFileRepository);
        inOrder.verify(directoryRepository).findAllDescendantIds(List.of(1L, 2L));
        inOrder.verify(virtualFileRepository).countActiveByDirectoryIds(List.of(1L, 2L, 3L));
        inOrder.verify(virtualFileRepository).softDeleteByDirectoryIds(List.of(1L, 2L, 3L));
        inOrder.verify(realFileRepository).decrementReferenceCount(50L, 3);
        inOrder.verify(realFileRepository).decrementReferenceCount(60L, 1);
        inOrder.verify(directoryRepository).softDeleteInBatch(List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("하위 디렉터리가 없으면 후속 삭제 로직을 수행하지 않는다")
    void handle_WhenNoDescendants_FinishesWithoutDelete() {
        // 대상 디렉터리가 없거나 이미 삭제된 경우 후속 삭제 로직은 건너뛰어야 함
        BatchJobQueue root = newJob("virtual_directories", 100L);
        when(directoryRepository.findAllDescendantIds(List.of(100L))).thenReturn(List.of());

        handler.handle(List.of(root));

        verify(directoryRepository).findAllDescendantIds(List.of(100L));
        verify(virtualFileRepository, never()).countActiveByDirectoryIds(anyList());
        verify(virtualFileRepository, never()).softDeleteByDirectoryIds(anyList());
        verifyNoInteractions(realFileRepository);
        verify(directoryRepository, never()).softDeleteInBatch(anyList());
    }

    private BatchJobQueue newJob(String targetTable, Long targetId) {
        return BatchJobQueue.builder()
                .jobType("DIRECTORY_DELETE")
                .targetTable(targetTable)
                .targetId(targetId)
                .jobData(Map.of())
                .maxAttempts(3)
                .nextRunAt(LocalDateTime.now())
                .build();
    }
}
