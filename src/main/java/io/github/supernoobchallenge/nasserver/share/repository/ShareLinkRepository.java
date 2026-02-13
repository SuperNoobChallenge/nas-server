package io.github.supernoobchallenge.nasserver.share.repository;

import io.github.supernoobchallenge.nasserver.share.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByShareUuid(String shareUuid);
    List<ShareLink> findAllByUser_IdAndLinkTypeAndDeletedAtIsNull(Long userId, String linkType);
    Optional<ShareLink> findTopByUser_IdAndLinkTypeAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, String linkType);
}
