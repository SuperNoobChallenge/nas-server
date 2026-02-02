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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobWorker {
        private final BatchJobQueueRepository batchJobQueueRepository;
        private final List<BatchJobHandler> batchJobHandlers; // 스프링이 모든 핸들러 구현체를 리스트로 주입해줌

        private Map<String, BatchJobHandler> handlerMap;

        // [초기화] 리스트로 받은 핸들러들을 "JobType"을 키로 하는 Map으로 변환 (검색 속도 향상)
        @PostConstruct
        public void init() {
        handlerMap = batchJobHandlers.stream()
                .collect(Collectors.toMap(BatchJobHandler::getJobType, Function.identity()));
        }

        // [스케줄링] 5초마다 실행 (이전 작업 끝나고 5초 뒤 실행)
        @Scheduled(fixedDelay = 5000)
        @Transactional // 전체 로직을 하나의 트랜잭션으로 묶거나, 내부 메서드에서 분리 가능
        public void processPendingJobs() {

        // 1. [Polling] 대기 중인 작업 100개 가져오기
        List<BatchJobQueue> jobs = batchJobQueueRepository.findTop100ByStatusOrderByBatchJobIdAsc("WAIT");

        if (jobs.isEmpty()) {
            return; // 일감이 없으면 바로 퇴근
        }

        log.info(">>> [BatchWorker] 대기 중인 작업 {}개 발견. 처리를 시작합니다.", jobs.size());

        // 2. [Locking] 가져온 작업들의 상태를 'PROCESSING'으로 즉시 변경 (중복 실행 방지)
        List<Long> allIds = jobs.stream().map(BatchJobQueue::getBatchJobId).toList();
        batchJobQueueRepository.updateStatusToProcessing(allIds, "PROCESSING");

        // 3. [Grouping] 작업 타입별로 분류 (DELETE끼리, UPDATE끼리...)
        Map<String, List<BatchJobQueue>> jobsByType = jobs.stream()
                .collect(Collectors.groupingBy(BatchJobQueue::getJobType));

        // 4. [Routing & Execution] 타입별로 핸들러에게 뭉텅이 전달
        jobsByType.forEach((type, jobList) -> {

            BatchJobHandler handler = handlerMap.get(type);
            List<Long> currentIds = jobList.stream().map(BatchJobQueue::getBatchJobId).toList();

            // 핸들러가 없으면 FAIL 처리
            if (handler == null) {
                log.error(">>> [Error] 핸들러를 찾을 수 없음: {}", type);
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

                // 5. [Fail] 실패 시 상태 일괄 업데이트 (Retry Count 증가 등)
                batchJobQueueRepository.markAsFail(currentIds); // 쿼리 한 방
            }
        });

        log.info(">>> [BatchWorker] 작업 {}개 처리 완료.", jobs.size());
    }
}
