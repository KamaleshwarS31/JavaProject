package com.evoting.dto.response;

import com.evoting.core.domain.enums.BallotState;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Ballot status response.
 * SECURITY: Does NOT include candidate choice, voter identity, or nullifier.
 * Only the commitment and lifecycle state are returned.
 */
@Data
@Builder
public class BallotStatusResponse {
    private String ballotReference;
    private String electionId;
    private BallotState lifecycleState;
    private String commitment;  // Commitment hash — not revealing choice
    private String batchReference;
    private String merkleRoot;
    private String blockchainTxId;
    private Instant acceptedAt;
    private Instant anchoredAt;
}
