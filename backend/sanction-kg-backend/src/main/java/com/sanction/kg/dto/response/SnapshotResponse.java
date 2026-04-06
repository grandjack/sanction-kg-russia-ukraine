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
public class SnapshotResponse {

    private Long id;
    private String source;
    private String fileHash;
    private Integer recordCount;
    private String status;
    private Date createdAt;
    private Date completedAt;
}
