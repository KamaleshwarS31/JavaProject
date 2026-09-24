package com.evoting.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ballot submission request DTO.
 *
 * SECURITY: This DTO must NEVER contain candidate_id or candidate_name in plaintext.
 * The candidate selection is encrypted in encryptedBallot.
 *
 * The ballot gateway receives:
 * - election ID (to identify the election)
 * - credential token (anonymous, for proof-of-eligibility)
 * - nullifier (election-specific, for double-vote prevention)
 * - encrypted ballot (RSA-OAEP encrypted candidate selection)
 * - request ID (for idempotency)
 *
 * The ballot gateway does NOT receive voter identity information.
 */
@Data
@NoArgsConstructor
public class BallotSubmissionRequest {

    @NotBlank(message = "Election ID is required")
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Election ID must be a valid UUID")
    private String electionId;

    @NotBlank(message = "Credential token is required")
    @Size(max = 512, message = "Credential token exceeds maximum length")
    private String credentialToken; // Anonymous credential — verified against credentials table

    @NotBlank(message = "Nullifier is required")
    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Nullifier must be a 64-character hex string (SHA-256)")
    private String nullifier; // N = SHA256(secret || electionId)

    @NotBlank(message = "Encrypted ballot is required")
    @Size(max = 8192, message = "Encrypted ballot payload too large")
    private String encryptedBallot; // RSA-OAEP encrypted, Base64-encoded

    @NotBlank(message = "Commitment is required")
    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Commitment must be a 64-character hex string")
    private String commitment; // C = SHA256(encryptedBallot || salt)

    @NotBlank(message = "Request ID is required for idempotency")
    @Size(max = 64)
    private String requestId; // X-Request-Id — idempotency key
}
