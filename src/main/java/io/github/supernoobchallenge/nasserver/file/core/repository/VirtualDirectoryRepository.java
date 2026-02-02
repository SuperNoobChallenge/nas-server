package io.github.supernoobchallenge.nasserver.file.core.repository;

import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VirtualDirectoryRepository extends JpaRepository<VirtualDirectory, Long> {
    // 1. [재귀 조회] 루트 ID들을 주면, 그 밑에 딸린 모든 자손 ID까지 다 긁어오기
    @Query(value = """
        WITH RECURSIVE CTE AS (
            SELECT directory_id FROM virtual_directories WHERE directory_id IN :rootIds
            UNION ALL
            SELECT v.directory_id FROM virtual_directories v
            INNER JOIN CTE c ON v.parent_directory_id = c.directory_id
        )
        SELECT directory_id FROM CTE
    """, nativeQuery = true)
    List<Long> findAllDescendantIds(@Param("rootIds") List<Long> rootIds);


    // 2. [일괄 Soft Delete] ID 목록에 해당하는 폴더들의 deleted_at을 현재 시간으로 업데이트
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VirtualDirectory v SET v.deletedAt = CURRENT_TIMESTAMP WHERE v.id IN :ids")
    void softDeleteInBatch(@Param("ids") List<Long> ids);
}
