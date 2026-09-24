package com.evoting.core.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Election-specific nullifier generator.
 *
 * <p>Protocol: N = H(secret || electionId)</p>
 *
 * <p>Properties:</p>
 * <ul>
 *   <li>Deterministic for the same (secret, electionId) pair</li>
 *   <li>Different across elections (electionId is included)</li>
 *   <li>Not directly reversible to voter identity</li>
 *   <li>Election-specific — different elections produce different nullifiers for same credential</li>
 * </ul>
 *
 * <p>IMPORTANT: This does NOT protect against credential theft.
 * If the credential secret 's' is stolen, the attacker can generate a valid nullifier.
 * See THREAT_MODEL.md for full analysis.</p>
 */
@Component
public class NullifierEngine {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String DOMAIN_SEPARATOR = "EVOTING_NULLIFIER_V1";

    /**
     * Generates an election-specific nullifier hash.
     *
     * @param credentialSecret the voter's protected credential secret (never stored plaintext)
     * @param electionId       the canonical election identifier (UUID string)
     * @return hex-encoded SHA-256 nullifier
     * @throws IllegalArgumentException if inputs are null or blank
     */
    public String generateNullifier(String credentialSecret, String electionId) {
        if (credentialSecret == null || credentialSecret.isBlank()) {
            throw new IllegalArgumentException("Credential secret must not be null or blank");
        }
        if (electionId == null || electionId.isBlank()) {
            throw new IllegalArgumentException("Election ID must not be null or blank");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // Domain-separated input: DOMAIN_SEPARATOR || electionId || secret
            // Domain separation prevents cross-protocol attacks
            String input = DOMAIN_SEPARATOR + "|" + electionId + "|" + credentialSecret;
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all Java SE implementations
            throw new IllegalStateException("SHA-256 not available in this JVM — critical security failure", e);
        }
    }

    /**
     * Validates that a provided nullifier matches the expected value.
     * Used for client-side verification only — the canonical check is database-level uniqueness.
     */
    public boolean verifyNullifier(String credentialSecret, String electionId, String claimedNullifier) {
        String expectedNullifier = generateNullifier(credentialSecret, electionId);
        // Use MessageDigest.isEqual for constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
            expectedNullifier.getBytes(StandardCharsets.UTF_8),
            claimedNullifier.getBytes(StandardCharsets.UTF_8)
        );
    }
}
