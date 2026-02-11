package io.github.supernoobchallenge.nasserver.batch.handler.impl;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.batch.handler.BatchJobHandler;
import io.github.supernoobchallenge.nasserver.file.capacity.entity.CapacityAllocation;
import io.github.supernoobchallenge.nasserver.file.capacity.repository.CapacityAllocationRepository;
import io.github.supernoobchallenge.nasserver.file.capacity.service.CapacityAllocationService;
import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilePermissionCapacityApplyHandler implements BatchJobHandler {
    private static final String TARGET_TABLE = "file_permission_keys";

    private final FilePermissionKeyRepository filePermissionKeyRepository;
    private final CapacityAllocationRepository capacityAllocationRepository;

    @Override
    public String getJobType() {
        return CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY;
    }

    @Override
    @Transactional
    public void handle(List<BatchJobQueue> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        boolean hasInvalidTargetTable = jobs.stream()
                .map(BatchJobQueue::getTargetTable)
                .anyMatch(targetTable -> !TARGET_TABLE.equals(targetTable));
        if (hasInvalidTargetTable) {
            throw new IllegalArgumentException("Invalid target_table for FILE_PERMISSION_CAPACITY_APPLY job");
        }

        List<CapacityAllocation> createdAllocations = new ArrayList<>();

        for (BatchJobQueue job : jobs) {
            Map<String, Object> data = job.getJobData();
            Long receiverPermissionId = toLong(data.get("receiverPermissionId"), "receiverPermissionId", true);
            Long giverPermissionId = toLong(data.get("giverPermissionId"), "giverPermissionId", false);
            long amount = toPositiveLong(data.get("amount"), "amount");
            String operation = toStringValue(data.get("operation"));
            String allocationType = toStringValue(data.get("allocationType"));
            String description = toStringValue(data.get("description"));

            if ("GRANT".equals(operation)) {
                applyGrant(receiverPermissionId, giverPermissionId, amount);
            } else if ("REVOKE".equals(operation)) {
                applyRevoke(receiverPermissionId, giverPermissionId, amount);
            } else {
                throw new IllegalArgumentException("operation은 GRANT 또는 REVOKE여야 합니다.");
            }

            createdAllocations.add(
                    CapacityAllocation.builder()
                            .receiverPermission(filePermissionKeyRepository.getReferenceById(receiverPermissionId))
                            .giverPermission(giverPermissionId == null ? null : filePermissionKeyRepository.getReferenceById(giverPermissionId))
                            .allocatedSize(amount)
                            .allocationType(allocationType == null || allocationType.isBlank() ? operation : allocationType)
                            .description(description)
                            .build()
            );
        }

        capacityAllocationRepository.saveAll(createdAllocations);
        log.info(">>> [배치 완료] file_permission capacity apply {}건", createdAllocations.size());
    }

    private void applyGrant(Long receiverPermissionId, Long giverPermissionId, long amount) {
        PermissionPair pair = lockPair(receiverPermissionId, giverPermissionId, "부여");
        FilePermissionKey receiverPermission = pair.receiverPermission();

        if (pair.giverPermission() != null) {
            FilePermissionKey giverPermission = pair.giverPermission();
            giverPermission.adjustAvailableCapacity(-amount);
        }

        receiverPermission.grantCapacity(amount);
    }

    private void applyRevoke(Long receiverPermissionId, Long giverPermissionId, long amount) {
        PermissionPair pair = lockPair(receiverPermissionId, giverPermissionId, "회수");
        FilePermissionKey receiverPermission = pair.receiverPermission();
        receiverPermission.revokeCapacity(amount);

        if (pair.giverPermission() != null) {
            FilePermissionKey giverPermission = pair.giverPermission();
            giverPermission.adjustAvailableCapacity(amount);
        }
    }

    private PermissionPair lockPair(Long receiverPermissionId, Long giverPermissionId, String actionKorean) {
        if (giverPermissionId != null && giverPermissionId.equals(receiverPermissionId)) {
            throw new IllegalArgumentException("giverPermissionId와 receiverPermissionId가 동일할 수 없습니다.");
        }

        if (giverPermissionId == null) {
            FilePermissionKey receiver = filePermissionKeyRepository.findByIdForUpdate(receiverPermissionId)
                    .orElseThrow(() -> new IllegalArgumentException(actionKorean + " 대상 권한 키가 없습니다."));
            return new PermissionPair(receiver, null);
        }

        Long first = Math.min(receiverPermissionId, giverPermissionId);
        Long second = Math.max(receiverPermissionId, giverPermissionId);

        FilePermissionKey firstLocked = filePermissionKeyRepository.findByIdForUpdate(first)
                .orElseThrow(() -> new IllegalArgumentException("권한 키를 찾을 수 없습니다. id=" + first));
        FilePermissionKey secondLocked = filePermissionKeyRepository.findByIdForUpdate(second)
                .orElseThrow(() -> new IllegalArgumentException("권한 키를 찾을 수 없습니다. id=" + second));

        if (receiverPermissionId.equals(first)) {
            return new PermissionPair(firstLocked, secondLocked);
        }
        return new PermissionPair(secondLocked, firstLocked);
    }

    private Long toLong(Object value, String field, boolean required) {
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException(field + "는 필수입니다.");
            }
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException(field + "는 숫자여야 합니다.");
    }

    private long toPositiveLong(Object value, String field) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(field + "는 숫자여야 합니다.");
        }
        long parsed = number.longValue();
        if (parsed <= 0L) {
            throw new IllegalArgumentException(field + "는 1 이상이어야 합니다.");
        }
        return parsed;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record PermissionPair(FilePermissionKey receiverPermission, FilePermissionKey giverPermission) {
    }
}
