package com.evoting.dto.response;

import com.evoting.core.domain.enums.ElectionState;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElectionResponse {
    private String id;
    private String electionCode;
    private String title;
    private String description;
    private ElectionState state;
    private String electionType;
    private String publicKeyPem; // RSA public key for client-side ballot encryption
    private Instant startTime;
    private Instant endTime;
    private boolean votingOpen;
    private long totalBallotsAccepted;
    private long totalBatchesAnchored;
    private List<CandidateResponse> candidates;
    private Instant createdAt;
    private Instant updatedAt;
}
