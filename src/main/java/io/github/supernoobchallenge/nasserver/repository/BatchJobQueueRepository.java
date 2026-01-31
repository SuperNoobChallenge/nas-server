package io.github.supernoobchallenge.nasserver.repository;

import io.github.supernoobchallenge.nasserver.entity.BatchJobQueue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchJobQueueRepository extends JpaRepository<BatchJobQueue, Long> {
}
