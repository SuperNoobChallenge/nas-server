package io.github.supernoobchallenge.nasserver.file.core.repository;

import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VirtualFileRepository extends JpaRepository<VirtualFile, Long> {

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE VirtualFile vf
            SET vf.deletedAt = CURRENT_TIMESTAMP
            WHERE vf.directory.id IN :directoryIds
              AND vf.deletedAt IS NULL
            """)
    void softDeleteByDirectoryIds(@Param("directoryIds") List<Long> directoryIds);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE VirtualFile vf
            SET vf.deletedAt = CURRENT_TIMESTAMP
            WHERE vf.id IN :virtualFileIds
              AND vf.deletedAt IS NULL
            """)
    void softDeleteByVirtualFileIds(@Param("virtualFileIds") List<Long> virtualFileIds);

    @Query("""
            SELECT vf.realFile.id, COUNT(vf)
            FROM VirtualFile vf
            WHERE vf.id IN :virtualFileIds
              AND vf.deletedAt IS NULL
            GROUP BY vf.realFile.id
            """)
    List<Object[]> countActiveByRealFileIds(@Param("virtualFileIds") Collection<Long> virtualFileIds);

    @Query("""
            SELECT vf.realFile.id, COUNT(vf)
            FROM VirtualFile vf
            WHERE vf.directory.id IN :directoryIds
              AND vf.deletedAt IS NULL
            GROUP BY vf.realFile.id
            """)
    List<Object[]> countActiveByDirectoryIds(@Param("directoryIds") Collection<Long> directoryIds);
}
