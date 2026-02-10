package io.github.supernoobchallenge.nasserver.batch.handler.impl;

import io.github.supernoobchallenge.nasserver.batch.handler.BatchJobHandler;
import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.file.core.repository.RealFileRepository;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
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
public class VirtualDirectoryDeleteHandler implements BatchJobHandler {
    private static final String JOB_TYPE = "DIRECTORY_DELETE";
    private static final String TARGET_TABLE = "virtual_directories";

    private final VirtualDirectoryRepository directoryRepository;
    private final VirtualFileRepository virtualFileRepository;
    private final RealFileRepository realFileRepository;

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    @Transactional // JPA 벌크 연산은 트랜잭션 필수
    public void handle(List<BatchJobQueue> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        boolean hasInvalidTargetTable = jobs.stream()
                .map(BatchJobQueue::getTargetTable)
                .anyMatch(targetTable -> !TARGET_TABLE.equals(targetTable));
        if (hasInvalidTargetTable) {
            throw new IllegalArgumentException("Invalid target_table for DIRECTORY_DELETE job");
        }

        // 1. 요청된 타겟 ID 추출
        List<Long> rootIds = jobs.stream()
                .map(BatchJobQueue::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (rootIds.isEmpty()) {
            log.warn("DIRECTORY_DELETE 작업에 유효한 targetId가 없습니다. jobCount={}", jobs.size());
            return;
        }

        // 2. 자손 ID까지 싹 다 긁어오기 (속도 빠름)
        List<Long> allTargetIds = directoryRepository.findAllDescendantIds(rootIds);

        if (allTargetIds.isEmpty()) {
            log.info("삭제할 대상 폴더가 없습니다 (이미 삭제되었거나 존재하지 않음).");
            return;
        }

        // 3. 삭제 대상 파일 기준으로 real_file 참조 카운트 차감량 산출
        List<Object[]> referenceCounts = virtualFileRepository.countActiveByDirectoryIds(allTargetIds);

        // 4. 하위 디렉터리에 속한 파일 soft delete
        virtualFileRepository.softDeleteByDirectoryIds(allTargetIds);

        // 5. real_file reference_count 감소 (음수 방지 쿼리)
        for (Object[] row : referenceCounts) {
            Long realFileId = (Long) row[0];
            int decrementBy = ((Long) row[1]).intValue();
            realFileRepository.decrementReferenceCount(realFileId, decrementBy);
        }

        // 6. 디렉터리 soft delete
        directoryRepository.softDeleteInBatch(allTargetIds);

        log.info(">>> [배치 완료] 요청 루트: {}개 -> 실제 삭제(하위포함): {}개", rootIds.size(), allTargetIds.size());
    }
}
