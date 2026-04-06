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
public class RiskPathResponse {

    private String entityId;
    private List<PathInfo> paths;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PathInfo {
        private Long id;
        private String pathJson;
        private Double contributionWeight;
        private Integer rankOrder;
    }
}
