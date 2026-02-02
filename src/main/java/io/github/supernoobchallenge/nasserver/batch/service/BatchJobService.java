package io.github.supernoobchallenge.nasserver.batch.service;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.batch.repository.BatchJobQueueRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BatchJobService {
    private final BatchJobQueueRepository repository;

    @Transactional
    public BatchJobQueue registerJob(String jobType, String targetTable, Long targetId, Map<String, Object> data) {
        BatchJobQueue job = BatchJobQueue.builder()
                .jobType(jobType)
                .targetTable(targetTable)
                .targetId(targetId)
                .jobData(data)
                .build();

        return repository.save(job);
    }
}
