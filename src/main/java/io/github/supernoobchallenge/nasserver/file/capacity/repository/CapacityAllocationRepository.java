package io.github.supernoobchallenge.nasserver.file.capacity.repository;

import io.github.supernoobchallenge.nasserver.file.capacity.entity.CapacityAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapacityAllocationRepository extends JpaRepository<CapacityAllocation, Long> {
}
