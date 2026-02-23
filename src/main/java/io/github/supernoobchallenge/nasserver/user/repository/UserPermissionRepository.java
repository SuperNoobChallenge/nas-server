package io.github.supernoobchallenge.nasserver.user.repository;

import io.github.supernoobchallenge.nasserver.user.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO user_permissions (
                user_id,
                can_invite_person,
                can_share_file,
                can_store_personal,
                can_create_group,
                created_at,
                updated_at,
                created_by,
                updated_by
            ) VALUES (
                :userId,
                :canInvitePerson,
                :canShareFile,
                :canStorePersonal,
                :canCreateGroup,
                NOW(),
                NOW(),
                :auditUserId,
                :auditUserId
            )
            """, nativeQuery = true)
    int insertSystemUserPermission(@Param("userId") Long userId,
                                   @Param("canInvitePerson") boolean canInvitePerson,
                                   @Param("canShareFile") boolean canShareFile,
                                   @Param("canStorePersonal") boolean canStorePersonal,
                                   @Param("canCreateGroup") boolean canCreateGroup,
                                   @Param("auditUserId") Long auditUserId);
}
