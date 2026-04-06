package com.sanction.kg.repository.mariadb;

import com.sanction.kg.entity.relational.RiskPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskPathRepository extends JpaRepository<RiskPath, Long> {

    List<RiskPath> findByEntityIdOrderByRankOrderAsc(String entityId);

    List<RiskPath> findByRiskResultIdOrderByRankOrderAsc(Long riskResultId);

    void deleteByRiskResultId(Long riskResultId);
}
