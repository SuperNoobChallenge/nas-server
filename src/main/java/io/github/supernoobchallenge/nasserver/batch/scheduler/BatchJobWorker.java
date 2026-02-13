package io.github.supernoobchallenge.nasserver.batch.scheduler;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.batch.handler.BatchJobHandler;
import io.github.supernoobchallenge.nasserver.batch.repository.BatchJobQueueRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobWorker {
    private static final int MAX_RETRY_COUNT = 4;
    private final BatchJobQueueRepository batchJobQueueRepository;
    private final List<BatchJobHandler> batchJobHandlers; // 스프링이 모든 핸들러 구현체를 리스트로 주입해줌

    private Map<String, BatchJobHandler> handlerMap;

    // [초기화] 리스트로 받은 핸들러들을 "JobType"을 키로 하는 Map으로 변환 (검색 속도 향상)
    @PostConstruct
    public void init() {
        handlerMap = batchJobHandlers.stream()
                .collect(Collectors.toMap(
                        BatchJobHandler::getJobType,
                        Function.identity(),
                        (existing, duplicate) -> {
                            throw new IllegalStateException(
                                    "Duplicate BatchJobHandler for jobType: " + existing.getJobType()
                            );
                        }
                ));
    }

    // [스케줄링] 1초마다 실행
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processPendingJobs() {
        // 1. [Polling] 대기/재시도 상태 중 "실행 가능한" 작업만 조회
        LocalDateTime now = LocalDateTime.now();
        List<BatchJobQueue> jobs = batchJobQueueRepository
                .findTop200ByStatusInAndNextRunAtLessThanEqualOrderByIdAsc(List.of("wait", "retry_wait"), now)
                .stream()
                .limit(100)
                .toList();

        if (jobs.isEmpty()) {
            return; // 일감이 없으면 바로 퇴근
        }

        log.info(">>> [BatchWorker] 대기 중인 작업 {}개 발견. 처리를 시작합니다.", jobs.size());

        // 2. [Locking] 가져온 작업들의 상태를 'PROCESSING'으로 즉시 변경 (중복 실행 방지)
        List<Long> allIds = jobs.stream().map(BatchJobQueue::getId).toList();
        int updatedCount = batchJobQueueRepository.updateStatusToProcessing(allIds, "in_progress");

        if (updatedCount == 0) {
            log.info(">>> [BatchWorker] 다른 워커가 이미 가져간 작업입니다. 이번 사이클은 종료합니다.");
            return;
        }

        // 2-1. 실제로 PROCESSING으로 바뀐 작업만 다시 조회
        List<BatchJobQueue> processingJobs = batchJobQueueRepository.findByIdInAndStatus(allIds, "in_progress");

        if (processingJobs.isEmpty()) {
            log.info(">>> [BatchWorker] 처리 가능한 작업이 없습니다. 이번 사이클은 종료합니다.");
            return;
        }

        // 3. [Grouping] 작업 타입별로 분류 (DELETE끼리, UPDATE끼리...)
        Map<String, List<BatchJobQueue>> jobsByType = processingJobs.stream()
                .collect(Collectors.groupingBy(BatchJobQueue::getJobType));

        // 4. [Routing & Execution] 타입별로 핸들러에게 뭉텅이 전달
        jobsByType.forEach((type, jobList) -> {
            BatchJobHandler handler = handlerMap.get(type);
            List<Long> currentIds = jobList.stream().map(BatchJobQueue::getId).toList();

            // 핸들러가 없으면 FAIL 처리
            if (handler == null) {
                log.error(">>> [Error] 핸들러를 찾을 수 없음: {}", type);
                // 핸들러가 없으면 재시도 가치가 없으므로 FAIL 처리
                batchJobQueueRepository.markAsFail(currentIds); // 쿼리 한 방
                return;
            }

            try {
                // [핵심] 핸들러에게 리스트 통째로 넘김 (Bulk 처리)
                handler.handle(jobList);
                // 5. [Success] 성공 시 상태 일괄 업데이트
                batchJobQueueRepository.markAsSuccess(currentIds); // 쿼리 한 방
            } catch (Exception e) {
                log.error(">>> [Error] 배치 작업 처리 중 실패. Type: {}, Error: {}", type, e.getMessage(), e);

                // 5. [Fail] 실패 시 상태 일괄 업데이트 (재시도 가능 횟수 체크)
                List<BatchJobQueue> failJobs = jobList.stream()
                        .filter(job -> {
                            int maxAttempts = job.getMaxAttempts() > 0 ? job.getMaxAttempts() : MAX_RETRY_COUNT;
                            return job.getAttemptCount() >= maxAttempts;
                        })
                        .toList();

                List<BatchJobQueue> retryJobs = jobList.stream()
                        .filter(job -> {
                            int maxAttempts = job.getMaxAttempts() > 0 ? job.getMaxAttempts() : MAX_RETRY_COUNT;
                            return job.getAttemptCount() < maxAttempts;
                        })
                        .toList();

                if (!failJobs.isEmpty()) {
                    List<Long> failIds = failJobs.stream()
                            .map(BatchJobQueue::getId)
                            .toList();
                    batchJobQueueRepository.markAsFail(failIds); // 재시도 불가
                }

                if (!retryJobs.isEmpty()) {
                    Map<LocalDateTime, List<Long>> retryIdsByNextRunAt = retryJobs.stream()
                            .collect(Collectors.groupingBy(
                                    job -> now.plus(getBackoff(job.getAttemptCount())),
                                    Collectors.mapping(BatchJobQueue::getId, Collectors.toList())
                            ));

                    retryIdsByNextRunAt.forEach((nextRunAt, ids) ->
                            batchJobQueueRepository.markAsRetryWait(ids, nextRunAt)
                    );
                }
            }
        });

        log.info(">>> [BatchWorker] 작업 {}개 처리 완료.", processingJobs.size());
    }

    private Duration getBackoff(int attemptCount) {
        return switch (attemptCount) {
            case 0 -> Duration.ZERO;
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            case 3 -> Duration.ofMinutes(15);
            default -> Duration.ofHours(1);
        };
    }
}
