package io.github.supernoobchallenge.nasserver.file.core.repository;

import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectoryStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VirtualDirectoryStatsRepository extends JpaRepository<VirtualDirectoryStats, Long> {
}
