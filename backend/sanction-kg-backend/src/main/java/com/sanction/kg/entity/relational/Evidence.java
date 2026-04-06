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
@Table(name = "evidences")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String source;

    @Column(name = "evidence_span", nullable = false, columnDefinition = "TEXT")
    private String evidenceSpan;

    @Column(name = "event_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date eventTime;

    @Column(name = "crawl_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date crawlTime;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "extractor_version", length = 64)
    private String extractorVersion;

    @Column(name = "entity_ids", columnDefinition = "TEXT")
    private String entityIds;

    @Column(name = "relation_id", length = 128)
    private String relationId;

    @PrePersist
    public void prePersist() {
        if (confidence == null) {
            confidence = 0.0;
        }
        if (crawlTime == null) {
            crawlTime = new Date();
        }
    }
}
