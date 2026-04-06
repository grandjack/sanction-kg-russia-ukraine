package com.sanction.kg.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionTaskRequest {

    @NotBlank(message = "taskType is required")
    private String taskType;

    private Long sourceSnapshotId;

    private Integer totalDocs;
}
