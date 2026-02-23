package io.github.supernoobchallenge.nasserver.user.repository;

import io.github.supernoobchallenge.nasserver.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByEmail(String email);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);

    @Modifying
    @Query(value = """
            INSERT INTO users (
                user_id,
                file_permission_id,
                inviter_id,
                login_id,
                password,
                email,
                deleted_at,
                created_at,
                updated_at,
                created_by,
                updated_by
            ) VALUES (
                :userId,
                :filePermissionId,
                NULL,
                :loginId,
                :password,
                :email,
                NULL,
                NOW(),
                NOW(),
                :auditUserId,
                :auditUserId
            )
            """, nativeQuery = true)
    int insertSystemUser(@Param("userId") Long userId,
                         @Param("filePermissionId") Long filePermissionId,
                         @Param("loginId") String loginId,
                         @Param("password") String password,
                         @Param("email") String email,
                         @Param("auditUserId") Long auditUserId);
}
