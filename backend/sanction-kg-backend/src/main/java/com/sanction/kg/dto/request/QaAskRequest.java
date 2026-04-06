package com.sanction.kg.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaAskRequest {

    @NotBlank(message = "question is required")
    private String question;

    /**
     * Query mode: graph, semantic, or hybrid.
     * Defaults to hybrid if not specified.
     */
    private String mode;

    public String getMode() {
        if (mode == null || mode.trim().isEmpty()) {
            return "hybrid";
        }
        return mode;
    }
}
