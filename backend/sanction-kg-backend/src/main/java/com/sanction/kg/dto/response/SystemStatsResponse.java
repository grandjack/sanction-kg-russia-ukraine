package com.sanction.kg.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatsResponse {

    private Long totalEntities;
    private Long totalRelations;
    private Long totalEvidences;
    private Long totalSnapshots;
    private Map<String, Long> entitiesByType;
    private Map<String, Long> entitiesByStatus;
    private Long riskResultCount;
    private Long extractionTaskCount;
    private Long changeLogCount;
    private String neo4jStatus;
    private String mariaDbStatus;
}
