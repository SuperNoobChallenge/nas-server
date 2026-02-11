package io.github.supernoobchallenge.nasserver.batch.handler.impl;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.file.capacity.entity.CapacityAllocation;
import io.github.supernoobchallenge.nasserver.file.capacity.repository.CapacityAllocationRepository;
import io.github.supernoobchallenge.nasserver.file.capacity.service.CapacityAllocationService;
import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilePermissionCapacityApplyHandlerTest {

    @Mock
    private FilePermissionKeyRepository filePermissionKeyRepository;

    @Mock
    private CapacityAllocationRepository capacityAllocationRepository;

    @InjectMocks
    private FilePermissionCapacityApplyHandler handler;

    @Test
    @DisplayName("jobType은 FILE_PERMISSION_CAPACITY_APPLY를 반환한다")
    void getJobType_ReturnsCapacityApply() {
        assertEquals(CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY, handler.getJobType());
    }

    @Test
    @DisplayName("jobs가 비어 있으면 리포지토리를 호출하지 않는다")
    void handle_WhenJobsEmpty_DoesNothing() {
        handler.handle(List.of());
        verifyNoInteractions(filePermissionKeyRepository, capacityAllocationRepository);
    }

    @Test
    @DisplayName("target_table이 file_permission_keys가 아니면 예외를 던진다")
    void handle_WhenTargetTableInvalid_Throws() {
        BatchJobQueue invalidJob = newJob(
                "other_table",
                10L,
                Map.of("receiverPermissionId", 10L, "amount", 100L, "operation", "GRANT")
        );

        assertThrows(IllegalArgumentException.class, () -> handler.handle(List.of(invalidJob)));
        verifyNoInteractions(filePermissionKeyRepository, capacityAllocationRepository);
    }

    @Test
    @DisplayName("GRANT 작업이면 giver available 감소 후 receiver 총/가용 용량을 증가시키고 이력을 저장한다")
    void handle_WhenGrantJob_UpdatesCapacityAndSavesAllocation() {
        FilePermissionKey giver = org.mockito.Mockito.mock(FilePermissionKey.class);
        FilePermissionKey receiver = org.mockito.Mockito.mock(FilePermissionKey.class);
        FilePermissionKey receiverRef = FilePermissionKey.builder().ownerType(USER).build();
        FilePermissionKey giverRef = FilePermissionKey.builder().ownerType(USER).build();

        // lock 순서는 id 오름차순
        when(filePermissionKeyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(giver));
        when(filePermissionKeyRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(receiver));
        when(filePermissionKeyRepository.getReferenceById(20L)).thenReturn(receiverRef);
        when(filePermissionKeyRepository.getReferenceById(10L)).thenReturn(giverRef);

        BatchJobQueue job = newJob(
                "file_permission_keys",
                20L,
                Map.of(
                        "receiverPermissionId", 20L,
                        "giverPermissionId", 10L,
                        "amount", 300L,
                        "operation", "GRANT",
                        "allocationType", "GRANT",
                        "description", "테스트"
                )
        );

        handler.handle(List.of(job));

        InOrder order = inOrder(filePermissionKeyRepository, giver, receiver, capacityAllocationRepository);
        order.verify(filePermissionKeyRepository).findByIdForUpdate(10L);
        order.verify(filePermissionKeyRepository).findByIdForUpdate(20L);
        order.verify(giver).adjustAvailableCapacity(-300L);
        order.verify(receiver).grantCapacity(300L);
        order.verify(capacityAllocationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("REVOKE 작업에서 giver가 없으면 receiver 회수만 수행하고 giverPermission은 null로 저장한다")
    void handle_WhenRevokeWithoutGiver_RevokesAndSavesAllocationWithNullGiver() {
        FilePermissionKey receiver = org.mockito.Mockito.mock(FilePermissionKey.class);
        FilePermissionKey receiverRef = FilePermissionKey.builder().ownerType(USER).build();
        when(filePermissionKeyRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(receiver));
        when(filePermissionKeyRepository.getReferenceById(30L)).thenReturn(receiverRef);

        BatchJobQueue job = newJob(
                "file_permission_keys",
                30L,
                Map.of(
                        "receiverPermissionId", 30L,
                        "amount", 100L,
                        "operation", "REVOKE"
                )
        );

        handler.handle(List.of(job));

        verify(receiver).revokeCapacity(100L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CapacityAllocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(capacityAllocationRepository).saveAll(captor.capture());
        List<CapacityAllocation> saved = captor.getValue();
        assertEquals(1, saved.size());
        CapacityAllocation allocation = saved.get(0);
        assertNull(allocation.getGiverPermission());
        // allocationType 미지정 시 operation(REVOKE)로 대체되는지 검증
        assertEquals("REVOKE", allocation.getAllocationType());
        assertEquals(100L, allocation.getAllocatedSize());
    }

    private BatchJobQueue newJob(String targetTable, Long targetId, Map<String, Object> jobData) {
        return BatchJobQueue.builder()
                .jobType(CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY)
                .targetTable(targetTable)
                .targetId(targetId)
                .jobData(jobData)
                .maxAttempts(3)
                .nextRunAt(LocalDateTime.now())
                .build();
    }
}
