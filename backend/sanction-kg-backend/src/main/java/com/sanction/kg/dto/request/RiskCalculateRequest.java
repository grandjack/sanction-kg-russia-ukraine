package com.sanction.kg.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskCalculateRequest {

    @NotBlank(message = "entityId is required")
    private String entityId;

    private Double alphaWeight;
    private Double betaWeight;
    private Double gammaWeight;
    private Date timeWindowStart;
    private Date timeWindowEnd;
}
