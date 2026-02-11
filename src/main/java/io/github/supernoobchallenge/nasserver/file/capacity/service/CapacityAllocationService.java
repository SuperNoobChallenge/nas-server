package io.github.supernoobchallenge.nasserver.file.capacity.service;

import io.github.supernoobchallenge.nasserver.batch.service.BatchJobService;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CapacityAllocationService {
    public static final String JOB_TYPE_CAPACITY_APPLY = "FILE_PERMISSION_CAPACITY_APPLY";
    private static final String TARGET_TABLE = "file_permission_keys";

    private final BatchJobService batchJobService;
    private final FilePermissionKeyRepository filePermissionKeyRepository;

    @Transactional
    public void requestGrantCapacity(
            Long receiverPermissionId,
            Long giverPermissionId,
            long amount,
            String allocationType,
            String description,
            Long requestedBy
    ) {
        validateRequest(receiverPermissionId, amount, requestedBy);
        filePermissionKeyRepository.findById(receiverPermissionId)
                .orElseThrow(() -> new IllegalArgumentException("부여 대상 권한 키가 없습니다."));

        if (giverPermissionId != null) {
            filePermissionKeyRepository.findById(giverPermissionId)
                    .orElseThrow(() -> new IllegalArgumentException("부여자 권한 키가 없습니다."));
        }

        Map<String, Object> jobData = new HashMap<>();
        jobData.put("receiverPermissionId", receiverPermissionId);
        jobData.put("giverPermissionId", giverPermissionId);
        jobData.put("amount", amount);
        jobData.put("operation", "GRANT");
        jobData.put("allocationType", allocationType);
        jobData.put("description", description);

        batchJobService.registerJob(
                JOB_TYPE_CAPACITY_APPLY,
                TARGET_TABLE,
                receiverPermissionId,
                jobData,
                requestedBy
        );
    }

    @Transactional
    public void requestRevokeCapacity(
            Long receiverPermissionId,
            Long giverPermissionId,
            long amount,
            String allocationType,
            String description,
            Long requestedBy
    ) {
        validateRequest(receiverPermissionId, amount, requestedBy);
        filePermissionKeyRepository.findById(receiverPermissionId)
                .orElseThrow(() -> new IllegalArgumentException("회수 대상 권한 키가 없습니다."));

        if (giverPermissionId != null) {
            filePermissionKeyRepository.findById(giverPermissionId)
                    .orElseThrow(() -> new IllegalArgumentException("회수자 권한 키가 없습니다."));
        }

        Map<String, Object> jobData = new HashMap<>();
        jobData.put("receiverPermissionId", receiverPermissionId);
        jobData.put("giverPermissionId", giverPermissionId);
        jobData.put("amount", amount);
        jobData.put("operation", "REVOKE");
        jobData.put("allocationType", allocationType);
        jobData.put("description", description);

        batchJobService.registerJob(
                JOB_TYPE_CAPACITY_APPLY,
                TARGET_TABLE,
                receiverPermissionId,
                jobData,
                requestedBy
        );
    }

    private void validateRequest(Long receiverPermissionId, long amount, Long requestedBy) {
        if (receiverPermissionId == null) {
            throw new IllegalArgumentException("receiverPermissionId는 필수입니다.");
        }
        if (requestedBy == null) {
            throw new IllegalArgumentException("requestedBy는 필수입니다.");
        }
        if (amount <= 0L) {
            throw new IllegalArgumentException("amount는 1 이상이어야 합니다.");
        }
    }
}
