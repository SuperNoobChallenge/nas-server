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
@Table(name = "batch_job_queues", indexes = @Index(name = "idx_batch_job_queues_polling", columnList = "status, jobType, targetTable, createdBy"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchJobQueue extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_job_id")
    private Long batchJobId;

    @Column(nullable = false, length = 50)
    private String jobType;

    @Column(nullable = false, length = 64)
    private String targetTable;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 15)
    private String status; // WAIT, PROCESSING, FAIL, SUCCESS

    @Type(JsonType.class)
    @Column(name = "job_data", columnDefinition = "json", nullable = false)
    private Map<String, Object> jobData = new HashMap<>();

    @Column(nullable = false)
    private int retryCount = 0;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    // ==========================================
    // 1. Builder 생성자
    // ==========================================
    @Builder
    public BatchJobQueue(String jobType, String targetTable, Long targetId, Map<String, Object> jobData) {
        this.jobType = jobType;
        this.targetTable = targetTable;
        this.targetId = targetId;

        // 기본값 설정
        this.status = "WAIT";
        this.retryCount = 0;

        // 데이터가 없으면 빈 HashMap, 있으면 전달받은 Map 저장
        this.jobData = (jobData != null) ? jobData : new HashMap<>();
    }

    // ==========================================
    // 2. 비즈니스 로직 메서드 (상태 변경)
    // ==========================================

    /**
     * 작업 시작 (WAIT -> PROCESSING)
     * 배치 스케줄러가 작업을 가져갈 때 호출
     */
    public void startProcessing() {
        if (!"WAIT".equals(this.status) && !"FAIL".equals(this.status)) {
            // 이미 돌고 있거나 끝난 작업은 다시 시작 불가 (재시도 로직에 따라 FAIL은 허용할 수도 있음)
            throw new IllegalStateException("대기 중인 작업만 시작할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = "PROCESSING";
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 작업 성공 완료 (PROCESSING -> SUCCESS)
     */
    public void completeJob() {
        this.status = "SUCCESS";
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * 작업 실패 (PROCESSING -> FAIL)
     * 재시도 횟수 증가
     */
    public void failJob() {
        this.status = "FAIL";
        this.retryCount++;
        this.finishedAt = LocalDateTime.now(); // 이번 시도의 종료 시간
    }

    /**
     * 작업 데이터 추가 (편의 메서드)
     * 서비스 로직에서 map.put() 대신 사용
     */
    public void addJobData(String key, Object value) {
        this.jobData.put(key, value);
    }
}
