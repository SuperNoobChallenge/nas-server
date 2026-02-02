package io.github.supernoobchallenge.nasserver.file.core.repository;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilePermissionKeyRepository extends JpaRepository<FilePermissionKey, Long> {
}
