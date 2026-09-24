package com.evoting.core.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("NullifierEngine Tests")
class NullifierEngineTest {

    private NullifierEngine nullifierEngine;

    @BeforeEach
    void setUp() {
        nullifierEngine = new NullifierEngine();
    }

    @Test
    @DisplayName("Should generate deterministic nullifier for same inputs")
    void testDeterministicNullifier() {
        String secret = "test-secret-abc123";
        String electionId = "550e8400-e29b-41d4-a716-446655440000";

        String n1 = nullifierEngine.generateNullifier(secret, electionId);
        String n2 = nullifierEngine.generateNullifier(secret, electionId);

        assertThat(n1).isEqualTo(n2);
    }

    @Test
    @DisplayName("Should generate different nullifiers for different elections")
    void testElectionSpecificity() {
        String secret = "same-secret";
        String election1 = "election-001";
        String election2 = "election-002";

        String n1 = nullifierEngine.generateNullifier(secret, election1);
        String n2 = nullifierEngine.generateNullifier(secret, election2);

        assertThat(n1).isNotEqualTo(n2);
    }

    @Test
    @DisplayName("Should generate different nullifiers for different secrets (same election)")
    void testSecretSpecificity() {
        String secret1 = "secret-voter-A";
        String secret2 = "secret-voter-B";
        String electionId = "election-001";

        String n1 = nullifierEngine.generateNullifier(secret1, electionId);
        String n2 = nullifierEngine.generateNullifier(secret2, electionId);

        assertThat(n1).isNotEqualTo(n2);
    }

    @Test
    @DisplayName("Nullifier should be 64-character hex string (SHA-256)")
    void testNullifierFormat() {
        String nullifier = nullifierEngine.generateNullifier("secret", "election-1");
        assertThat(nullifier).matches("[a-f0-9]{64}");
    }

    @Test
    @DisplayName("Should reject null secret")
    void testNullSecretRejected() {
        assertThatThrownBy(() -> nullifierEngine.generateNullifier(null, "election-1"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject null electionId")
    void testNullElectionIdRejected() {
        assertThatThrownBy(() -> nullifierEngine.generateNullifier("secret", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Verify should return true for matching nullifier")
    void testVerifyMatchingNullifier() {
        String secret = "voter-secret";
        String electionId = "election-x";
        String nullifier = nullifierEngine.generateNullifier(secret, electionId);

        assertThat(nullifierEngine.verifyNullifier(secret, electionId, nullifier)).isTrue();
    }

    @Test
    @DisplayName("Verify should return false for non-matching nullifier")
    void testVerifyNonMatchingNullifier() {
        assertThat(nullifierEngine.verifyNullifier("secret", "election-1", "deadbeef")).isFalse();
    }
}
