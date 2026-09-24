package com.evoting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Merkle inclusion proof for public verification.
 * A voter can verify their commitment is in the published Merkle tree
 * WITHOUT revealing their vote choice.
 */
@Data
@Builder
public class MerkleProofResponse {
    private String commitment;
    private String merkleRoot;
    private String batchReference;
    private String blockchainTxId;
    private int leafIndex;
    private List<ProofElementDto> proof;
    private boolean verified;
    private String verificationMessage;

    @Data
    @Builder
    public static class ProofElementDto {
        private String siblingHash;
        private boolean isLeft;
    }
}
