package io.github.supernoobchallenge.nasserver.user.repository;

import io.github.supernoobchallenge.nasserver.user.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
}
