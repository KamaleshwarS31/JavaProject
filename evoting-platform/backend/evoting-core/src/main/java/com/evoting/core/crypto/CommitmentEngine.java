package com.evoting.core.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generates privacy-preserving ballot commitments.
 *
 * <p>Protocol: C = H(encryptedBallot || randomSalt)</p>
 *
 * <p>The commitment:
 * <ul>
 *   <li>Binds the ballot to a cryptographic value without revealing content</li>
 *   <li>Is included as a leaf in the Merkle batch tree</li>
 *   <li>Is published in the voter receipt for independent verification</li>
 *   <li>Does NOT reveal candidate selection</li>
 * </ul>
 * </p>
 */
@Component
public class CommitmentEngine {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String DOMAIN_SEPARATOR = "EVOTING_COMMITMENT_V1";
    private static final int SALT_BYTES = 32; // 256-bit salt

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a random salt for commitment creation.
     * Must be stored (associated with the ballot_reference) to allow future Merkle proof generation.
     *
     * @return hex-encoded 256-bit random salt
     */
    public String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Creates a cryptographic commitment to an encrypted ballot.
     *
     * @param encryptedBallotHex the hex-encoded encrypted ballot payload
     * @param salt               the hex-encoded random salt (from generateSalt())
     * @return hex-encoded SHA-256 commitment
     */
    public String createCommitment(String encryptedBallotHex, String salt) {
        if (encryptedBallotHex == null || encryptedBallotHex.isBlank()) {
            throw new IllegalArgumentException("Encrypted ballot must not be null or blank");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("Salt must not be null or blank");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String input = DOMAIN_SEPARATOR + "|" + encryptedBallotHex + "|" + salt;
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Verifies a commitment matches the expected value.
     * Used by the public verification portal.
     */
    public boolean verifyCommitment(String encryptedBallotHex, String salt, String claimedCommitment) {
        String expected = createCommitment(encryptedBallotHex, salt);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            claimedCommitment.getBytes(StandardCharsets.UTF_8)
        );
    }
}
