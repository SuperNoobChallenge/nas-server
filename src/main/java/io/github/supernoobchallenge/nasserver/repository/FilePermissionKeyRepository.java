package io.github.supernoobchallenge.nasserver.repository;

import io.github.supernoobchallenge.nasserver.entity.FilePermissionKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilePermissionKeyRepository extends JpaRepository<FilePermissionKey, Long> {
}
