package com.sanction.kg.entity.graph;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Represents an entity node in the Neo4j knowledge graph.
 * This is a POJO mapped from Neo4j query results (not a Spring Data Neo4j entity,
 * since we use the Neo4j Java Driver directly).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityNode {

    private String entityId;
    private String name;
    private String aliases;
    private String entityType;
    private String sanctionStatus;
    private Double riskScore;
    private Integer communityId;
    private Double pagerankScore;
    private Double hawkesIntensity;
    private Integer createdVersion;
    private Integer updatedVersion;
    private List<String> labels;

    /**
     * Parse aliases string into a list.
     */
    public List<String> getAliasList() {
        if (aliases == null || aliases.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String[] parts = aliases.split(",");
        List<String> result = new java.util.ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
