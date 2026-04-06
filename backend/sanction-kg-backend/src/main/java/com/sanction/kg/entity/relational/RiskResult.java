package com.sanction.kg.entity.relational;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "risk_results")
public class RiskResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false, length = 128)
    private String entityId;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Column(name = "ppr_score", nullable = false)
    private Double pprScore;

    @Column(name = "comm_risk_score", nullable = false)
    private Double commRiskScore;

    @Column(name = "hawkes_score", nullable = false)
    private Double hawkesScore;

    @Column(name = "alpha_weight", nullable = false)
    private Double alphaWeight;

    @Column(name = "beta_weight", nullable = false)
    private Double betaWeight;

    @Column(name = "gamma_weight", nullable = false)
    private Double gammaWeight;

    @Column(name = "time_window_start")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timeWindowStart;

    @Column(name = "time_window_end")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timeWindowEnd;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "calculated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date calculatedAt;

    @PrePersist
    public void prePersist() {
        if (calculatedAt == null) {
            calculatedAt = new Date();
        }
        if (version == null) {
            version = 1;
        }
        if (alphaWeight == null) alphaWeight = 0.33;
        if (betaWeight == null) betaWeight = 0.33;
        if (gammaWeight == null) gammaWeight = 0.34;
    }
}
