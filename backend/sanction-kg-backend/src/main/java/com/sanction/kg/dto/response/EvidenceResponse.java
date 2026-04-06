package com.sanction.kg.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceResponse {

    private Long id;
    private String source;
    private String evidenceSpan;
    private Date eventTime;
    private Date crawlTime;
    private Double confidence;
    private String extractorVersion;
    private String entityIds;
    private String relationId;
}
