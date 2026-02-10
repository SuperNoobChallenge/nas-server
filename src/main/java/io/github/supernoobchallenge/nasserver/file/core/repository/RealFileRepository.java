package io.github.supernoobchallenge.nasserver.file.core.repository;

import io.github.supernoobchallenge.nasserver.file.core.entity.RealFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RealFileRepository extends JpaRepository<RealFile, Long> {

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RealFile rf
            SET rf.referenceCount =
                CASE
                    WHEN rf.referenceCount >= :decrementBy THEN rf.referenceCount - :decrementBy
                    ELSE 0
                END
            WHERE rf.realFileId = :realFileId
            """)
    void decrementReferenceCount(@Param("realFileId") Long realFileId, @Param("decrementBy") int decrementBy);
}
