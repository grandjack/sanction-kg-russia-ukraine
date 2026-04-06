package com.sanction.kg.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityDetailResponse {

    private String entityId;
    private String name;
    private List<String> aliases;
    private String entityType;
    private String sanctionStatus;
    private Double riskScore;
    private Integer communityId;
    private Double pagerankScore;
    private Double hawkesIntensity;
    private Integer createdVersion;
    private Integer updatedVersion;
    private List<String> labels;
    private List<RelationInfo> relations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelationInfo {
        private String relationType;
        private String targetEntityId;
        private String targetName;
        private String targetType;
        private Double weight;
        private String direction;
        private String firstSeenAt;
        private String lastSeenAt;
    }
}
