package io.github.supernoobchallenge.nasserver.file.capacity.integration;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.batch.handler.impl.FilePermissionCapacityApplyHandler;
import io.github.supernoobchallenge.nasserver.batch.repository.BatchJobQueueRepository;
import io.github.supernoobchallenge.nasserver.batch.service.BatchJobService;
import io.github.supernoobchallenge.nasserver.file.capacity.entity.CapacityAllocation;
import io.github.supernoobchallenge.nasserver.file.capacity.repository.CapacityAllocationRepository;
import io.github.supernoobchallenge.nasserver.file.capacity.service.CapacityAllocationService;
import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import io.github.supernoobchallenge.nasserver.global.config.JpaConfig;
import io.github.supernoobchallenge.nasserver.global.security.AuditorAwareImpl;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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
        FilePermissionCapacityApplyHandler.class
})
class CapacityAllocationBatchIntegrationTest {

    @Autowired
    private CapacityAllocationService capacityAllocationService;

    @Autowired
    private FilePermissionCapacityApplyHandler filePermissionCapacityApplyHandler;

    @Autowired
    private FilePermissionKeyRepository filePermissionKeyRepository;

    @Autowired
    private CapacityAllocationRepository capacityAllocationRepository;

    @Autowired
    private BatchJobQueueRepository batchJobQueueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("용량 부여 요청은 배치 작업으로 등록되고, 핸들러 실행 시 receiver/giver 용량과 이력이 반영된다")
    void grantCapacity_IsAppliedByBatchHandler() {
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
                200L,
                "GRANT",
                "integration-grant",
                99L
        );

        List<BatchJobQueue> jobs = batchJobQueueRepository.findAll().stream()
                .filter(job -> CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY.equals(job.getJobType()))
                .toList();
        assertThat(jobs).hasSize(1);

        filePermissionCapacityApplyHandler.handle(jobs);
        entityManager.flush();
        entityManager.clear();

        FilePermissionKey updatedGiver = filePermissionKeyRepository.findById(giver.getId()).orElseThrow();
        FilePermissionKey updatedReceiver = filePermissionKeyRepository.findById(receiver.getId()).orElseThrow();
        assertThat(updatedGiver.getTotalCapacity()).isEqualTo(1_000L);
        assertThat(updatedGiver.getAvailableCapacity()).isEqualTo(800L);
        assertThat(updatedReceiver.getTotalCapacity()).isEqualTo(300L);
        assertThat(updatedReceiver.getAvailableCapacity()).isEqualTo(300L);

        List<CapacityAllocation> allocations = capacityAllocationRepository.findAll();
        assertThat(allocations).hasSize(1);
        CapacityAllocation allocation = allocations.get(0);
        assertThat(allocation.getReceiverPermission().getId()).isEqualTo(receiver.getId());
        assertThat(allocation.getGiverPermission().getId()).isEqualTo(giver.getId());
        assertThat(allocation.getAllocatedSize()).isEqualTo(200L);
        assertThat(allocation.getAllocationType()).isEqualTo("GRANT");
    }

    @Test
    @DisplayName("용량 회수 요청은 배치 핸들러 실행 시 receiver 용량을 회수하고 이력을 남긴다")
    void revokeCapacity_IsAppliedByBatchHandler() {
        FilePermissionKey receiver = FilePermissionKey.builder().ownerType(USER).build();
        receiver.grantCapacity(500L);
        filePermissionKeyRepository.save(receiver);
        entityManager.flush();
        entityManager.clear();

        capacityAllocationService.requestRevokeCapacity(
                receiver.getId(),
                null,
                120L,
                "SYSTEM",
                "integration-revoke",
                42L
        );

        List<BatchJobQueue> jobs = batchJobQueueRepository.findAll().stream()
                .filter(job -> CapacityAllocationService.JOB_TYPE_CAPACITY_APPLY.equals(job.getJobType()))
                .toList();
        assertThat(jobs).hasSize(1);

        filePermissionCapacityApplyHandler.handle(jobs);
        entityManager.flush();
        entityManager.clear();

        FilePermissionKey updatedReceiver = filePermissionKeyRepository.findById(receiver.getId()).orElseThrow();
        assertThat(updatedReceiver.getTotalCapacity()).isEqualTo(380L);
        assertThat(updatedReceiver.getAvailableCapacity()).isEqualTo(380L);

        List<CapacityAllocation> allocations = capacityAllocationRepository.findAll();
        assertThat(allocations).hasSize(1);
        CapacityAllocation allocation = allocations.get(0);
        assertThat(allocation.getReceiverPermission().getId()).isEqualTo(receiver.getId());
        assertThat(allocation.getGiverPermission()).isNull();
        assertThat(allocation.getAllocatedSize()).isEqualTo(120L);
        assertThat(allocation.getAllocationType()).isEqualTo("SYSTEM");
    }
}
