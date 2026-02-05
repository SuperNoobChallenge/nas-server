package io.github.supernoobchallenge.nasserver.batch.handler.impl;

import io.github.supernoobchallenge.nasserver.batch.handler.BatchJobHandler;
import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectoryDeleteHandler implements BatchJobHandler {

    private final VirtualDirectoryRepository directoryRepository;

    @Override
    public String getJobType() {
        return "DIRECTORY_DELETE";
    }

    @Override
    @Transactional // JPA 벌크 연산은 트랜잭션 필수
    public void handle(List<BatchJobQueue> jobs) {
        if (jobs.isEmpty()) return;

        // 1. 요청된 타겟 ID 추출
        List<Long> rootIds = jobs.stream()
                .map(BatchJobQueue::getTargetId)
                .toList();

        // 2. 자손 ID까지 싹 다 긁어오기 (속도 빠름)
        List<Long> allTargetIds = directoryRepository.findAllDescendantIds(rootIds);

        if (allTargetIds.isEmpty()) {
            log.info("삭제할 대상 폴더가 없습니다 (이미 삭제되었거나 존재하지 않음).");
            return;
        }

        // 3. [JPQL] 벌크 업데이트 수행
        directoryRepository.softDeleteInBatch(allTargetIds);

        log.info(">>> [배치 완료] 요청 루트: {}개 -> 실제 삭제(하위포함): {}개", rootIds.size(), allTargetIds.size());
    }
}
