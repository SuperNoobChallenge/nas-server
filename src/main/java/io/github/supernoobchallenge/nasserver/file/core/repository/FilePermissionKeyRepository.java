package io.github.supernoobchallenge.nasserver.file.core.repository;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FilePermissionKeyRepository extends JpaRepository<FilePermissionKey, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FilePermissionKey f where f.id = :id")
    Optional<FilePermissionKey> findByIdForUpdate(@Param("id") Long id);
}
