package com.evoting.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Privacy-preserving voting receipt.
 *
 * SECURITY GUARANTEE: This receipt MUST NOT:
 * - Reveal the candidate choice
 * - Allow proving candidate choice to another person
 * - Become a coercion tool
 *
 * The commitment is a SHA-256 hash that can be used to verify
 * Merkle tree inclusion without revealing the vote.
 */
@Data
@Builder
public class ReceiptResponse {
    private String receiptId;
    private String electionId;
    private String electionTitle;
    private String ballotReference;   // Opaque receipt identifier
    private String commitment;        // For Merkle verification
    private String batchReference;    // Batch this ballot is in
    private String merkleRoot;        // Batch Merkle root
    private String blockchainTxId;    // Fabric transaction ID for independent verification
    private String verificationUrl;   // URL for public verification portal
    // NOTE: candidateSelection is intentionally ABSENT from this record
}
