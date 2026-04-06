package com.sanction.kg.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotCreateRequest {

    @NotBlank(message = "source is required")
    private String source;

    private String fileHash;

    private Integer recordCount;
}
