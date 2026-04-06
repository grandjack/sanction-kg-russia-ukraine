package com.sanction.kg.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskResultResponse {

    private Long id;
    private String entityId;
    private String entityName;
    private Double riskScore;
    private Double pprScore;
    private Double commRiskScore;
    private Double hawkesScore;
    private Double alphaWeight;
    private Double betaWeight;
    private Double gammaWeight;
    private Date timeWindowStart;
    private Date timeWindowEnd;
    private Integer version;
    private Date calculatedAt;
    private List<RiskPathInfo> paths;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskPathInfo {
        private Long id;
        private String pathJson;
        private Double contributionWeight;
        private Integer rankOrder;
    }
}
