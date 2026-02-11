package io.github.supernoobchallenge.nasserver.file.capacity.service;

import io.github.supernoobchallenge.nasserver.batch.service.BatchJobService;
import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapacityAllocationServiceTest {

    @Mock
    private BatchJobService batchJobService;

    @Mock
    private FilePermissionKeyRepository filePermissionKeyRepository;

    @InjectMocks
    private CapacityAllocationService capacityAllocationService;

    @Test
    @DisplayName("용량 부여 요청은 배치 큐에 GRANT 작업을 등록한다")
    void requestGrantCapacity_RegistersBatchJob() {
        FilePermissionKey receiver = FilePermissionKey.builder().ownerType(USER).build();
        FilePermissionKey giver = FilePermissionKey.builder().ownerType(USER).build();
        when(filePermissionKeyRepository.findById(10L)).thenReturn(Optional.of(receiver));
        when(filePermissionKeyRepository.findById(20L)).thenReturn(Optional.of(giver));

        capacityAllocationService.requestGrantCapacity(
                10L,
                20L,
                512L,
                "GRANT",
                "테스트 부여",
                1L
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> jobDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(batchJobService).registerJob(
                eq(CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY),
                eq("file_permission_keys"),
                eq(10L),
                jobDataCaptor.capture(),
                eq(1L)
        );

        Map<String, Object> jobData = jobDataCaptor.getValue();
        assertEquals(10L, jobData.get("receiverPermissionId"));
        assertEquals(20L, jobData.get("giverPermissionId"));
        assertEquals(512L, jobData.get("amount"));
        assertEquals("GRANT", jobData.get("operation"));
        assertEquals("GRANT", jobData.get("allocationType"));
    }

    @Test
    @DisplayName("용량 회수 요청은 배치 큐에 REVOKE 작업을 등록한다")
    void requestRevokeCapacity_RegistersBatchJob() {
        FilePermissionKey receiver = FilePermissionKey.builder().ownerType(USER).build();
        when(filePermissionKeyRepository.findById(10L)).thenReturn(Optional.of(receiver));

        capacityAllocationService.requestRevokeCapacity(
                10L,
                null,
                128L,
                "SYSTEM",
                "테스트 회수",
                7L
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> jobDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(batchJobService).registerJob(
                eq(CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY),
                eq("file_permission_keys"),
                eq(10L),
                jobDataCaptor.capture(),
                eq(7L)
        );

        Map<String, Object> jobData = jobDataCaptor.getValue();
        assertEquals("REVOKE", jobData.get("operation"));
        assertEquals(128L, jobData.get("amount"));
        assertEquals("SYSTEM", jobData.get("allocationType"));
    }

    @Test
    @DisplayName("요청 용량이 0 이하이면 배치 작업을 등록하지 않고 예외를 던진다")
    void requestGrantCapacity_WhenAmountInvalid_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                capacityAllocationService.requestGrantCapacity(
                        10L, 20L, 0L, "GRANT", "invalid", 1L
                )
        );

        verify(batchJobService, never()).registerJob(
                eq(CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY),
                eq("file_permission_keys"),
                eq(10L),
                anyMap(),
                eq(1L)
        );
    }
}
