package com.sanction.kg.repository.mariadb;

import com.sanction.kg.entity.relational.RiskResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskResultRepository extends JpaRepository<RiskResult, Long> {

    /**
     * Find the latest risk result for an entity.
     */
    @Query("SELECT r FROM RiskResult r WHERE r.entityId = :entityId ORDER BY r.calculatedAt DESC")
    List<RiskResult> findLatestByEntityId(@Param("entityId") String entityId, Pageable pageable);

    /**
     * Find all risk results for an entity ordered by calculation time.
     */
    List<RiskResult> findByEntityIdOrderByCalculatedAtDesc(String entityId);

    /**
     * Get top N entities by risk score (latest results only).
     * MariaDB 10.1 compatible: no window functions, use subquery approach.
     */
    @Query("SELECT r FROM RiskResult r WHERE r.id IN " +
            "(SELECT MAX(r2.id) FROM RiskResult r2 GROUP BY r2.entityId) " +
            "ORDER BY r.riskScore DESC")
    List<RiskResult> findTopRiskyEntities(Pageable pageable);
}
