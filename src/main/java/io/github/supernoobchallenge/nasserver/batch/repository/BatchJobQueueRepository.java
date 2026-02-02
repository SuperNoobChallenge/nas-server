package io.github.supernoobchallenge.nasserver.batch.repository;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BatchJobQueueRepository extends JpaRepository<BatchJobQueue, Long> {

    /**
     * 대기 중인 작업 N개 조회 (오래된 순)
     * Limit 기능은 메서드 이름(Top)으로 해결됨
     */
    List<BatchJobQueue> findTop100ByStatusOrderByBatchJobIdAsc(String status);

    /**
     * 가져온 작업들을 "처리 중"으로 한 방에 변경
     * - 중복 실행 방지 (Locking 역할)
     * - 시작 시간(startedAt) 기록
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BatchJobQueue b SET b.status = :status, b.startedAt = CURRENT_TIMESTAMP WHERE b.batchJobId IN :ids")
    void updateStatusToProcessing(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 성공한 작업들을 "성공"으로 한 방에 변경
     * - 종료 시간(finishedAt) 기록
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BatchJobQueue b SET b.status = 'SUCCESS', b.finishedAt = CURRENT_TIMESTAMP WHERE b.batchJobId IN :ids")
    void markAsSuccess(@Param("ids") List<Long> ids);

    /**
     * 실패한 작업들을 "실패"로 변경 및 재시도 카운트 증가
     * - 보통 실패는 개별적으로 처리하지만, 시스템 셧다운 등으로 일괄 실패 처리할 때 필요
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BatchJobQueue b SET b.status = 'FAIL', b.retryCount = b.retryCount + 1, b.finishedAt = CURRENT_TIMESTAMP WHERE b.batchJobId IN :ids")
    void markAsFail(@Param("ids") List<Long> ids);

    /**
     * 오래된 완료/실패 내역 삭제 (Hard Delete)
     * - 예: 30일 지난 로그 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM BatchJobQueue b WHERE b.updatedAt < :cutoffDate AND b.status IN ('SUCCESS', 'FAIL')")
    void deleteOldJobs(@Param("cutoffDate") LocalDateTime cutoffDate);
}
