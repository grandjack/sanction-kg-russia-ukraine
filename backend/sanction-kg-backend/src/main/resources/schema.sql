-- MariaDB 10.1 compatible DDL for sanction_kg database
-- No JSON type, no window functions, no CTEs

CREATE DATABASE IF NOT EXISTS sanction_kg
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE sanction_kg;

-- ----------------------------
-- Table: snapshots
-- Data import snapshot records
-- ----------------------------
CREATE TABLE IF NOT EXISTS snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(255) NOT NULL COMMENT 'Data source identifier (OFAC_SDN, EU_CONSOLIDATED, etc.)',
    file_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of source file',
    record_count INT NOT NULL DEFAULT 0 COMMENT 'Number of records in this snapshot',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PROCESSING, COMPLETED, FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_snapshots_source (source),
    INDEX idx_snapshots_status (status),
    INDEX idx_snapshots_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table: evidences
-- Evidence store for traceability
-- ----------------------------
CREATE TABLE IF NOT EXISTS evidences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(512) NOT NULL COMMENT 'Source URL or document reference',
    evidence_span TEXT NOT NULL COMMENT 'Extracted text span serving as evidence',
    event_time DATETIME NULL COMMENT 'When the event described by this evidence occurred',
    crawl_time DATETIME NOT NULL COMMENT 'When this evidence was collected',
    confidence DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Confidence score [0.0, 1.0]',
    extractor_version VARCHAR(64) NULL COMMENT 'Version of the extraction model',
    entity_ids TEXT NULL COMMENT 'Comma-separated entity IDs referenced by this evidence',
    relation_id VARCHAR(128) NULL COMMENT 'Relation ID this evidence supports',
    PRIMARY KEY (id),
    INDEX idx_evidences_source (source(191)),
    INDEX idx_evidences_event_time (event_time),
    INDEX idx_evidences_confidence (confidence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table: risk_results
-- HGT-RAM risk assessment output
-- ----------------------------
CREATE TABLE IF NOT EXISTS risk_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    entity_id VARCHAR(128) NOT NULL COMMENT 'Entity identifier from Neo4j',
    risk_score DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Fused risk score',
    ppr_score DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Personalized PageRank score',
    comm_risk_score DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Community risk score',
    hawkes_score DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Hawkes process temporal score',
    alpha_weight DOUBLE NOT NULL DEFAULT 0.33 COMMENT 'Weight for PPR component',
    beta_weight DOUBLE NOT NULL DEFAULT 0.33 COMMENT 'Weight for community component',
    gamma_weight DOUBLE NOT NULL DEFAULT 0.34 COMMENT 'Weight for Hawkes component',
    time_window_start DATETIME NULL COMMENT 'Start of assessment time window',
    time_window_end DATETIME NULL COMMENT 'End of assessment time window',
    version INT NOT NULL DEFAULT 1 COMMENT 'Assessment version number',
    calculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_risk_results_entity_id (entity_id),
    INDEX idx_risk_results_risk_score (risk_score),
    INDEX idx_risk_results_version (version),
    INDEX idx_risk_results_calculated_at (calculated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table: risk_paths
-- Risk propagation paths
-- ----------------------------
CREATE TABLE IF NOT EXISTS risk_paths (
    id BIGINT NOT NULL AUTO_INCREMENT,
    entity_id VARCHAR(128) NOT NULL COMMENT 'Target entity ID',
    risk_result_id BIGINT NOT NULL COMMENT 'FK to risk_results',
    path_json TEXT NOT NULL COMMENT 'JSON-encoded path data (stored as TEXT for MariaDB 10.1)',
    contribution_weight DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Path contribution to total risk',
    rank_order INT NOT NULL DEFAULT 0 COMMENT 'Rank of this path by contribution',
    PRIMARY KEY (id),
    INDEX idx_risk_paths_entity_id (entity_id),
    INDEX idx_risk_paths_risk_result_id (risk_result_id),
    CONSTRAINT fk_risk_paths_result FOREIGN KEY (risk_result_id) REFERENCES risk_results(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table: extraction_tasks
-- GSR-ER extraction task tracking
-- ----------------------------
CREATE TABLE IF NOT EXISTS extraction_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_type VARCHAR(64) NOT NULL COMMENT 'ENTITY_EXTRACTION, RELATION_EXTRACTION, EVENT_EXTRACTION',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, COMPLETED, FAILED',
    source_snapshot_id BIGINT NULL COMMENT 'FK to snapshots',
    total_docs INT NOT NULL DEFAULT 0,
    processed_docs INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_extraction_tasks_status (status),
    INDEX idx_extraction_tasks_type (task_type),
    CONSTRAINT fk_extraction_tasks_snapshot FOREIGN KEY (source_snapshot_id) REFERENCES snapshots(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table: change_logs
-- Version management and audit trail
-- ----------------------------
CREATE TABLE IF NOT EXISTS change_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version_id INT NOT NULL COMMENT 'Graph version number',
    change_type VARCHAR(16) NOT NULL COMMENT 'CREATE, UPDATE, EXPIRE',
    target_type VARCHAR(32) NOT NULL COMMENT 'ENTITY, RELATION, EVENT',
    target_id VARCHAR(128) NOT NULL COMMENT 'ID of the changed entity/relation',
    before_value TEXT NULL COMMENT 'State before change (JSON as TEXT)',
    after_value TEXT NULL COMMENT 'State after change (JSON as TEXT)',
    trigger_source VARCHAR(255) NULL COMMENT 'What triggered this change',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_change_logs_version_id (version_id),
    INDEX idx_change_logs_target (target_type, target_id),
    INDEX idx_change_logs_change_type (change_type),
    INDEX idx_change_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
