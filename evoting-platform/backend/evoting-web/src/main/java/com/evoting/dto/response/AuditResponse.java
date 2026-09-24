package com.evoting.dto.response;

import com.evoting.core.domain.enums.ElectionState;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AuditResponse {
    private String electionId;
    private String electionCode;
    private String title;
    private ElectionState state;
    private long totalBallotsAccepted;
    private long totalBatchesAnchored;
    private List<BatchSummary> batches;
    private List<AnchorSummary> auditAnchors;
    private boolean auditMismatchDetected;

    @Data
    @Builder
    public static class BatchSummary {
        private String batchReference;
        private String merkleRoot;
        private int leafCount;
        private String blockchainTxId;
        private String status;
        private Instant anchoredAt;
    }

    @Data
    @Builder
    public static class AnchorSummary {
        private String anchorReference;
        private String auditDigest;
        private String blockchainTxId;
        private String verificationStatus;
        private boolean mismatchDetected;
        private Instant createdAt;
    }
}
