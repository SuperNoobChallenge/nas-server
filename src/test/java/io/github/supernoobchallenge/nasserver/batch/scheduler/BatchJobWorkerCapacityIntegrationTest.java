package io.github.supernoobchallenge.nasserver.batch.scheduler;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.batch.repository.BatchJobQueueRepository;
import io.github.supernoobchallenge.nasserver.batch.service.BatchJobService;
import io.github.supernoobchallenge.nasserver.file.capacity.entity.CapacityAllocation;
import io.github.supernoobchallenge.nasserver.file.capacity.repository.CapacityAllocationRepository;
import io.github.supernoobchallenge.nasserver.file.capacity.service.CapacityAllocationService;
import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import io.github.supernoobchallenge.nasserver.global.config.JpaConfig;
import io.github.supernoobchallenge.nasserver.global.security.AuditorAwareImpl;
import io.github.supernoobchallenge.nasserver.batch.handler.impl.FilePermissionCapacityApplyHandler;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.USER;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaConfig.class,
        AuditorAwareImpl.class,
        BatchJobService.class,
        CapacityAllocationService.class,
        FilePermissionCapacityApplyHandler.class,
        BatchJobWorker.class
})
class BatchJobWorkerCapacityIntegrationTest {

    @Autowired
    private CapacityAllocationService capacityAllocationService;

    @Autowired
    private BatchJobWorker batchJobWorker;

    @Autowired
    private FilePermissionKeyRepository filePermissionKeyRepository;

    @Autowired
    private BatchJobQueueRepository batchJobQueueRepository;

    @Autowired
    private CapacityAllocationRepository capacityAllocationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("워커 실행 시 FILE_PERMISSION_CAPACITY_APPLY 작업이 success로 완료되고 용량/이력이 반영된다")
    void processPendingJobs_CompletesCapacityJobAndAppliesChanges() {
        FilePermissionKey giver = FilePermissionKey.builder().ownerType(USER).build();
        giver.grantCapacity(1_000L);
        FilePermissionKey receiver = FilePermissionKey.builder().ownerType(USER).build();
        receiver.grantCapacity(100L);
        filePermissionKeyRepository.save(giver);
        filePermissionKeyRepository.save(receiver);
        entityManager.flush();
        entityManager.clear();

        capacityAllocationService.requestGrantCapacity(
                receiver.getId(),
                giver.getId(),
                250L,
                "GRANT",
                "worker-integration",
                5L
        );
        entityManager.flush();
        entityManager.clear();

        BatchJobQueue queuedJob = batchJobQueueRepository.findAll().stream()
                .filter(job -> CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY.equals(job.getJobType()))
                .findFirst()
                .orElseThrow();
        assertThat(queuedJob.getStatus()).isEqualTo("wait");
        assertThat(((Number) queuedJob.getJobData().get("giverPermissionId")).longValue()).isEqualTo(giver.getId());

        batchJobWorker.processPendingJobs();
        entityManager.flush();
        entityManager.clear();

        BatchJobQueue processedJob = batchJobQueueRepository.findById(queuedJob.getId()).orElseThrow();
        assertThat(processedJob.getStatus()).isEqualTo("success");
        assertThat(processedJob.getAttemptCount()).isEqualTo(1);
        assertThat(processedJob.getFinishedAt()).isNotNull();

        FilePermissionKey updatedGiver = filePermissionKeyRepository.findById(giver.getId()).orElseThrow();
        FilePermissionKey updatedReceiver = filePermissionKeyRepository.findById(receiver.getId()).orElseThrow();
        assertThat(updatedGiver.getAvailableCapacity()).isEqualTo(750L);
        assertThat(updatedReceiver.getTotalCapacity()).isEqualTo(350L);
        assertThat(updatedReceiver.getAvailableCapacity()).isEqualTo(350L);

        List<CapacityAllocation> allocations = capacityAllocationRepository.findAll();
        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).getAllocatedSize()).isEqualTo(250L);
        assertThat(allocations.get(0).getGiverPermission().getId()).isEqualTo(giver.getId());
    }
}
