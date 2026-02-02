package io.github.supernoobchallenge.nasserver.batch.repository;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BatchJobQueueRepository extends JpaRepository<BatchJobQueue, Long> {
    // 가장 오래된 대기 작업 1개 조회
    Optional<BatchJobQueue> findFirstByStatusOrderByBatchJobIdAsc(String status);
}
