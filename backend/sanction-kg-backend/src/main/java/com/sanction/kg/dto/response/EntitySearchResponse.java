package com.sanction.kg.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntitySearchResponse {

    private List<EntitySummary> items;
    private Integer page;
    private Integer size;
    private Long total;
    private Integer totalPages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EntitySummary {
        private String entityId;
        private String name;
        private List<String> aliases;
        private String entityType;
        private String sanctionStatus;
        private Double riskScore;
    }
}
