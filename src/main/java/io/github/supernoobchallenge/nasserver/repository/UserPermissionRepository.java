package io.github.supernoobchallenge.nasserver.repository;

import io.github.supernoobchallenge.nasserver.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
}
