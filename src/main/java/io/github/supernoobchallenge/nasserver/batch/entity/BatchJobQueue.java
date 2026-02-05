package io.github.supernoobchallenge.nasserver.batch.entity;

import io.github.supernoobchallenge.nasserver.global.entity.AuditEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(
        name = "batch_job_queues",
        indexes = {
                // DDL에 맞춰 실제 컬럼명으로 인덱스 지정
                @Index(name = "idx_batch_job_queues_polling", columnList = "status,next_run_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchJobQueue extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_job_queue_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "target_table", nullable = false, length = 64)
    private String targetTable;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "status", nullable = false, length = 15)
    private String status; // wait, in_progress, retry_wait, fail, success

    @Type(JsonType.class)
    @Column(name = "job_data", columnDefinition = "json", nullable = false)
    private Map<String, Object> jobData = new HashMap<>();

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime startedAt;

    // DDL에 started_at 컬럼이 없으므로 제거
    // finished_at은 존재
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // created_at / updated_at / created_by / updated_by 는 AuditEntity에서 매핑한다고 가정
    // (AuditEntity 컬럼명이 DDL과 다르면 AuditEntity 쪽도 같이 맞춰야 함)

    // ==========================================
    // 1. Builder 생성자
    // ==========================================
    @Builder
    public BatchJobQueue(
            String jobType,
            String targetTable,
            Long targetId,
            Map<String, Object> jobData,
            int maxAttempts,
            LocalDateTime nextRunAt
    ) {
        this.jobType = jobType;
        this.targetTable = targetTable;
        this.targetId = targetId;

        this.status = "wait";
        this.attemptCount = 0;

        this.maxAttempts = maxAttempts;
        this.nextRunAt = nextRunAt;

        this.jobData = (jobData != null) ? jobData : new HashMap<>();
    }

    // ==========================================
    // 2. 비즈니스 로직 메서드 (상태 변경)
    // ==========================================

    /**
     * 작업 시작 (wait/retry_wait -> in_progress)
     * 배치 스케줄러가 작업을 가져갈 때 호출
     */
    public void startProcessing() {
        if (!"wait".equals(this.status) && !"retry_wait".equals(this.status)) {
            throw new IllegalStateException("대기 중인 작업만 시작할 수 있습니다. 현재 상태: " + this.status);
        }
        // 시도 횟수는 "실행을 시도"할 때 증가시키는 게 자연스럽다.
        // (실패 시 증가시키면 '시도'가 아니라 '실패 횟수'에 가까워짐)
        this.attemptCount++;
        this.status = "in_progress";
    }

    /**
     * 작업 성공 완료 (in_progress -> success)
     */
    public void completeJob() {
        if (!"in_progress".equals(this.status)) {
            throw new IllegalStateException("진행 중인 작업만 완료할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = "success";
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * 작업 실패 처리 (in_progress -> retry_wait or fail)
     * - 다음 실행 시각(next_run_at)은 호출자가(서비스/스케줄러) 정책에 따라 계산해서 넘겨주는 형태 추천
     */
    public void failJob(LocalDateTime nextRunAtAfterBackoff) {
        if (!"in_progress".equals(this.status)) {
            throw new IllegalStateException("진행 중인 작업만 실패 처리할 수 있습니다. 현재 상태: " + this.status);
        }

        this.finishedAt = LocalDateTime.now();

        if (this.attemptCount >= this.maxAttempts) {
            this.status = "fail";
            // fail이면 next_run_at은 의미가 없지만, 남겨둬도 무방
            return;
        }

        this.status = "retry_wait";
        this.nextRunAt = nextRunAtAfterBackoff;
    }

    /**
     * 작업 데이터 추가(편의 메서드)
     */
    public void addJobData(String key, Object value) {
        this.jobData.put(key, value);
    }

    /**
     * 실행 가능 여부 (폴링에서 필터하기 전에 도메인 체크로도 활용 가능)
     */
    public boolean isRunnableAt(LocalDateTime now) {
        return ("wait".equals(this.status) || "retry_wait".equals(this.status))
                && (this.nextRunAt != null && !this.nextRunAt.isAfter(now));
    }
}
