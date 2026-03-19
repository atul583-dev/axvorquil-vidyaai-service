package com.axvorquil.vidya.dto;

import com.axvorquil.vidya.model.Vertical;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ReadinessRequest {

    @NotNull
    private Vertical vertical;

    @NotBlank
    private String exam;  // e.g. "SSC CGL 2026"

    /** subject name → mock test score (0–100) */
    @NotNull
    private Map<String, Integer> subjectScores;

    @Min(1)
    private int daysLeft;
}
