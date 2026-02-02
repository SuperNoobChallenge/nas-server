package io.github.supernoobchallenge.nasserver.file.core.repository;

import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VirtualDirectoryRepository extends JpaRepository<VirtualDirectory, Long> {
}
