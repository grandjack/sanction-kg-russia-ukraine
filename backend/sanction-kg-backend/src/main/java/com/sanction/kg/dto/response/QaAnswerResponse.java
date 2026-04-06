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
public class QaAnswerResponse {

    private String answer;
    private String mode;
    private Double confidence;
    private List<EvidenceReference> evidences;
    private GraphSubgraphResponse relatedSubgraph;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvidenceReference {
        private Long evidenceId;
        private String source;
        private String evidenceSpan;
        private Double confidence;
    }
}
