package com.sanction.kg.repository.mariadb;

import com.sanction.kg.entity.relational.Evidence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    /**
     * Search evidences where entity_ids column contains the given entityId.
     * Uses LIKE since MariaDB 10.1 doesn't have JSON functions.
     */
    @Query("SELECT e FROM Evidence e WHERE e.entityIds LIKE CONCAT('%', :entityId, '%') ORDER BY e.crawlTime DESC")
    Page<Evidence> findByEntityId(@Param("entityId") String entityId, Pageable pageable);

    @Query("SELECT e FROM Evidence e WHERE e.entityIds LIKE CONCAT('%', :entityId, '%') ORDER BY e.crawlTime DESC")
    List<Evidence> findAllByEntityId(@Param("entityId") String entityId);

    List<Evidence> findByRelationId(String relationId);

    @Query("SELECT e FROM Evidence e WHERE e.source LIKE CONCAT('%', :keyword, '%') OR e.evidenceSpan LIKE CONCAT('%', :keyword, '%') ORDER BY e.crawlTime DESC")
    Page<Evidence> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
