package com.sanction.kg.entity.relational;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "risk_paths")
public class RiskPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false, length = 128)
    private String entityId;

    @Column(name = "risk_result_id", nullable = false)
    private Long riskResultId;

    @Column(name = "path_json", nullable = false, columnDefinition = "TEXT")
    private String pathJson;

    @Column(name = "contribution_weight", nullable = false)
    private Double contributionWeight;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @PrePersist
    public void prePersist() {
        if (contributionWeight == null) {
            contributionWeight = 0.0;
        }
        if (rankOrder == null) {
            rankOrder = 0;
        }
    }
}
