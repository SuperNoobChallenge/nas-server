package io.github.supernoobchallenge.nasserver.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
public class BatchJobQueue extends AuditEntity{
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
}
