package io.github.supernoobchallenge.nasserver.batch.handler.impl;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.batch.handler.BatchJobHandler;
import io.github.supernoobchallenge.nasserver.file.core.repository.RealFileRepository;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class VirtualFileDeleteHandler implements BatchJobHandler {
    private static final String JOB_TYPE = "VIRTUAL_FILE_DELETE";
    private static final String TARGET_TABLE = "virtual_files";

    private final VirtualFileRepository virtualFileRepository;
    private final RealFileRepository realFileRepository;

    @Override
    public String getJobType() {
        return JOB_TYPE;
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
            throw new IllegalArgumentException("Invalid target_table for VIRTUAL_FILE_DELETE job");
        }

        List<Long> virtualFileIds = jobs.stream()
                .map(BatchJobQueue::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (virtualFileIds.isEmpty()) {
            log.warn("VIRTUAL_FILE_DELETE 작업에 유효한 targetId가 없습니다. jobCount={}", jobs.size());
            return;
        }

        List<Object[]> referenceCounts = virtualFileRepository.countActiveByRealFileIds(virtualFileIds);
        virtualFileRepository.softDeleteByVirtualFileIds(virtualFileIds);

        for (Object[] row : referenceCounts) {
            Long realFileId = (Long) row[0];
            int decrementBy = ((Long) row[1]).intValue();
            realFileRepository.decrementReferenceCount(realFileId, decrementBy);
        }

        log.info(">>> [배치 완료] virtual file soft delete {}건", virtualFileIds.size());
    }
}
