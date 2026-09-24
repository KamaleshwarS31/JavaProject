package com.evoting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CandidateResponse {
    private String id;
    private String candidateCode;
    private String displayName;
    private String biography;
    private String partyName;
    private String symbolUri;
    private int ballotOrder;
    private String status;
    private Instant createdAt;
}
