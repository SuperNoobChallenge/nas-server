package io.github.supernoobchallenge.nasserver.batch.handler.impl;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.file.core.repository.RealFileRepository;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

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
class VirtualFileDeleteHandlerTest {

    @Mock
    private VirtualFileRepository virtualFileRepository;

    @Mock
    private RealFileRepository realFileRepository;

    @InjectMocks
    private VirtualFileDeleteHandler handler;

    @Test
    @DisplayName("jobType은 VIRTUAL_FILE_DELETE를 반환한다")
    void getJobType_ReturnsVirtualFileDelete() {
        // Handler 라우팅 키가 기대값과 일치하는지 검증
        assertEquals("VIRTUAL_FILE_DELETE", handler.getJobType());
    }

    @Test
    @DisplayName("jobs가 비어 있으면 어떤 리포지토리도 호출하지 않는다")
    void handle_WhenJobsEmpty_DoesNothing() {
        // 입력이 비어 있으면 외부 의존성을 호출하지 않고 종료해야 함
        handler.handle(List.of());
        verifyNoInteractions(virtualFileRepository, realFileRepository);
    }

    @Test
    @DisplayName("target_table이 virtual_files가 아니면 예외를 던진다")
    void handle_WhenTargetTableInvalid_Throws() {
        // target_table이 잘못되면 즉시 예외로 차단해야 함
        BatchJobQueue invalidJob = newJob("virtual_directories", 10L);

        assertThrows(IllegalArgumentException.class, () -> handler.handle(List.of(invalidJob)));
        verifyNoInteractions(virtualFileRepository, realFileRepository);
    }

    @Test
    @DisplayName("정상 jobs면 파일 soft delete 후 real_file 참조 카운트를 감소시킨다")
    void handle_WhenValidJobs_DeletesFilesAndDecrementsReferenceCounts() {
        // 정상 케이스:
        // 1) real_file별 감소량 집계
        // 2) virtual_file soft delete
        // 3) real_file reference_count 감소
        BatchJobQueue j1 = newJob("virtual_files", 100L);
        BatchJobQueue j2 = newJob("virtual_files", 101L);
        List<Object[]> counts = List.of(
                new Object[]{11L, 2L},
                new Object[]{12L, 1L}
        );
        when(virtualFileRepository.countActiveByRealFileIds(anyList())).thenReturn(counts);

        handler.handle(List.of(j1, j2));

        InOrder inOrder = inOrder(virtualFileRepository, realFileRepository);
        inOrder.verify(virtualFileRepository).countActiveByRealFileIds(anyList());
        inOrder.verify(virtualFileRepository).softDeleteByVirtualFileIds(List.of(100L, 101L));
        inOrder.verify(realFileRepository).decrementReferenceCount(11L, 2);
        inOrder.verify(realFileRepository).decrementReferenceCount(12L, 1);
    }

    @Test
    @DisplayName("유효한 targetId가 없으면 삭제/카운트 감소를 수행하지 않는다")
    void handle_WhenNoValidTargetIds_DoesNotDelete() {
        // targetId가 모두 null이면 실제 삭제/카운트 감소 로직은 수행하지 않아야 함
        BatchJobQueue nullIdJob = newJob("virtual_files", null);

        handler.handle(List.of(nullIdJob));

        verify(virtualFileRepository, never()).countActiveByRealFileIds(anyList());
        verify(virtualFileRepository, never()).softDeleteByVirtualFileIds(anyList());
        verifyNoInteractions(realFileRepository);
    }

    private BatchJobQueue newJob(String targetTable, Long targetId) {
        return BatchJobQueue.builder()
                .jobType("VIRTUAL_FILE_DELETE")
                .targetTable(targetTable)
                .targetId(targetId)
                .jobData(Map.of())
                .maxAttempts(3)
                .nextRunAt(LocalDateTime.now())
                .build();
    }
}
