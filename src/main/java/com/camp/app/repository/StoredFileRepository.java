package com.camp.app.repository;

import com.camp.app.entity.StoredFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    boolean existsByParentId(Long parentId);

    /**
     * parentMode 0 只看根，1 看指定父目录。anyOwner 为 1 时不按拥有者过滤。
     * purposeLike 为空表示不按用途过滤，否则是 "PURPOSE/%"。
     */
    @Query("""
            SELECT f FROM StoredFile f
            WHERE f.deletedAt IS NULL
              AND (:anyOwner = 1 OR f.ownerId = :ownerId)
              AND (
                    (:parentMode = 0 AND f.parentId IS NULL)
                 OR (:parentMode = 1 AND f.parentId = :parentId)
              )
              AND (:purposeLike = '' OR f.storageKey LIKE :purposeLike)
            """)
    Page<StoredFile> search(@Param("anyOwner") int anyOwner,
                            @Param("ownerId") Long ownerId,
                            @Param("parentMode") int parentMode,
                            @Param("parentId") Long parentId,
                            @Param("purposeLike") String purposeLike,
                            Pageable pageable);
}
