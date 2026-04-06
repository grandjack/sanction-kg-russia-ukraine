package com.sanction.kg.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Graph subgraph response formatted for D3.js/ECharts visualization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphSubgraphResponse {

    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GraphNode {
        private String id;
        private String name;
        private String type;
        private Double riskScore;
        private String sanctionStatus;
        private Integer communityId;
        private List<String> labels;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GraphEdge {
        private String source;
        private String target;
        private String type;
        private Double weight;
        private String firstSeenAt;
        private String lastSeenAt;
    }
}
