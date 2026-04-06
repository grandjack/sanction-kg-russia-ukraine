package com.sanction.kg.exception;

/**
 * Centralized error codes for the application.
 */
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "Bad request"),
    NOT_FOUND(404, "Resource not found"),
    INTERNAL_ERROR(500, "Internal server error"),

    ENTITY_NOT_FOUND(1001, "Entity not found"),
    RISK_RESULT_NOT_FOUND(1002, "Risk result not found"),
    EVIDENCE_NOT_FOUND(1003, "Evidence not found"),
    SNAPSHOT_NOT_FOUND(1004, "Snapshot not found"),
    TASK_NOT_FOUND(1005, "Extraction task not found"),

    RISK_CALCULATION_FAILED(2001, "Risk calculation failed"),
    EXTRACTION_FAILED(2002, "Extraction task failed"),
    QA_SERVICE_ERROR(2003, "Q&A service error"),

    NEO4J_CONNECTION_ERROR(3001, "Neo4j connection error"),
    DB_ERROR(3002, "Database error"),

    INVALID_PARAMETER(4001, "Invalid parameter"),
    MISSING_PARAMETER(4002, "Missing required parameter");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
