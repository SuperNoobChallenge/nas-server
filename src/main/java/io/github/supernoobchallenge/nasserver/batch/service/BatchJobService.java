package io.github.supernoobchallenge.nasserver.batch.service;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.batch.repository.BatchJobQueueRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobService {
    private static final int DEFAULT_MAX_ATTEMPTS = 4;

    private final BatchJobQueueRepository batchJobQueueRepository;

    /**
     * [단건 등록] 새로운 배치 작업을 큐에 등록합니다.
     * * @param jobType     작업 종류 (예: DIRECTORY_DELETE)
     * @param targetTable 대상 테이블 (예: virtual_directories)
     * @param targetId    대상 ID (예: 100)
     * @param jobData     작업에 필요한 파라미터 (JSON으로 변환됨)
     * @param userId      작업 요청자 ID (Audit 용)
     */
    @Transactional
    public void registerJob(String jobType, String targetTable, Long targetId, Map<String, Object> jobData, Long userId) {

        BatchJobQueue job = BatchJobQueue.builder()
                .jobType(jobType)
                .targetTable(targetTable)
                .targetId(targetId)
                .jobData(jobData) // 초기 상태
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .nextRunAt(LocalDateTime.now())
                .build();

        batchJobQueueRepository.save(job);

        log.info(">>> [Batch Registered] JobType: {}, TargetId: {}", jobType, targetId);
    }

    /**
     * [다건 등록] 여러 작업을 한 번에 등록할 때 사용 (성능 최적화)
     */
    @Transactional
    public void registerJobs(List<BatchJobQueue> jobs) {
        batchJobQueueRepository.saveAll(jobs);
        log.info(">>> [Batch Bulk Registered] {} jobs registered.", jobs.size());
    }
}
