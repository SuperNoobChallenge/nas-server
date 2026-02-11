package io.github.supernoobchallenge.nasserver.batch.repository;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BatchJobQueueRepository extends JpaRepository<BatchJobQueue, Long> {

    /**
     * 대기 중인 작업 N개 조회 (오래된 순)
     * Limit 기능은 메서드 이름(Top)으로 해결됨
     */
    List<BatchJobQueue> findTop100ByStatusOrderByIdAsc(String status);

    /**
     * 대기/재시도 상태의 작업 N개 조회 (오래된 순)
     * 재시도 대상 선별은 워커에서 수행
     */
    List<BatchJobQueue> findTop200ByStatusInOrderByIdAsc(List<String> statuses);

    /**
     * 가져온 작업들을 "처리 중"으로 한 방에 변경
     * - 중복 실행 방지 (Locking 역할)
     * - 시도 횟수 증가
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BatchJobQueue b SET b.status = :status, b.attemptCount = b.attemptCount + 1, b.startedAt = CURRENT_TIMESTAMP WHERE b.id IN :ids AND b.status IN ('wait', 'retry_wait')")
    int updateStatusToProcessing(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 처리 중인 작업만 다시 조회 (동시성 제어용)
     */
    List<BatchJobQueue> findByIdInAndStatus(List<Long> ids, String status);

    /**
     * 성공한 작업들을 "성공"으로 한 방에 변경
     * - 종료 시간(finishedAt) 기록
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BatchJobQueue b SET b.status = 'success', b.finishedAt = CURRENT_TIMESTAMP WHERE b.id IN :ids")
    void markAsSuccess(@Param("ids") List<Long> ids);

    /**
     * 실패한 작업들을 "실패"로 변경
     * - 보통 실패는 개별적으로 처리하지만, 시스템 셧다운 등으로 일괄 실패 처리할 때 필요
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BatchJobQueue b SET b.status = 'fail', b.finishedAt = CURRENT_TIMESTAMP WHERE b.id IN :ids")
    void markAsFail(@Param("ids") List<Long> ids);

    /**
     * 실패한 작업들을 "재시도 대기"로 변경 및 재시도 카운트 증가
     * - 재시도 대상 선별은 워커에서 수행
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BatchJobQueue b SET b.status = 'retry_wait', b.nextRunAt = :nextRunAt, b.finishedAt = CURRENT_TIMESTAMP WHERE b.id IN :ids")
    void markAsRetryWait(@Param("ids") List<Long> ids, @Param("nextRunAt") LocalDateTime nextRunAt);

    /**
     * 오래된 완료/실패 내역 삭제 (Hard Delete)
     * - 예: 30일 지난 로그 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM BatchJobQueue b WHERE b.updatedAt < :cutoffDate AND b.status IN ('success', 'fail')")
    void deleteOldJobs(@Param("cutoffDate") LocalDateTime cutoffDate);
}
