package com.evoting.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CandidateRequest {

    @NotBlank
    @Size(min = 2, max = 20)
    @Pattern(regexp = "^[A-Z0-9]+$")
    private String candidateCode;

    @NotBlank
    @Size(max = 255)
    private String displayName;

    @Size(max = 5000)
    private String biography;

    @Size(max = 100)
    private String partyName;

    @Size(max = 500)
    private String symbolUri;

    @Min(0)
    private int ballotOrder;
}
