package com.sanction.kg.entity.graph;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Represents a relationship in the Neo4j knowledge graph.
 * Mapped from Neo4j query results using the Java Driver.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctionRelation {

    private String sourceId;
    private String targetId;
    private String type;
    private Double weight;
    private String firstSeenAt;
    private String lastSeenAt;
    private String evidenceIds;
    private Integer createdVersion;
}
