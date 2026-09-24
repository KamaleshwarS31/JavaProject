package com.evoting.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class ElectionCreateRequest {

    @NotBlank(message = "Election code is required")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Election code must be uppercase alphanumeric")
    private String electionCode;

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotBlank
    private String electionType; // SINGLE_CHOICE, MULTI_CHOICE, RANKED_CHOICE

    private Instant startTime;
    private Instant endTime;

    @Min(10) @Max(1000)
    private int batchSizeLimit = 100;
}
