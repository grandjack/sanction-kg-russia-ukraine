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
@Table(name = "extraction_tasks")
public class ExtractionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type", nullable = false, length = 64)
    private String taskType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "source_snapshot_id")
    private Long sourceSnapshotId;

    @Column(name = "total_docs", nullable = false)
    private Integer totalDocs;

    @Column(name = "processed_docs", nullable = false)
    private Integer processedDocs;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "completed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (totalDocs == null) totalDocs = 0;
        if (processedDocs == null) processedDocs = 0;
    }
}
