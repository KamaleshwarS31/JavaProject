package com.evoting.core.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MerkleTreeEngine Tests")
class MerkleTreeEngineTest {

    private MerkleTreeEngine merkleTreeEngine;

    @BeforeEach
    void setUp() {
        merkleTreeEngine = new MerkleTreeEngine();
    }

    @Test
    @DisplayName("Should compute consistent root for same input")
    void testDeterministicRoot() {
        List<String> commitments = List.of("commit-1", "commit-2", "commit-3", "commit-4");
        String root1 = merkleTreeEngine.computeRoot(commitments);
        String root2 = merkleTreeEngine.computeRoot(commitments);
        assertThat(root1).isEqualTo(root2);
    }

    @Test
    @DisplayName("Root should be 64-char hex string")
    void testRootFormat() {
        String root = merkleTreeEngine.computeRoot(List.of("c1", "c2"));
        assertThat(root).matches("[a-f0-9]{64}");
    }

    @Test
    @DisplayName("Different inputs should produce different roots")
    void testRootUniqueness() {
        String root1 = merkleTreeEngine.computeRoot(List.of("a", "b"));
        String root2 = merkleTreeEngine.computeRoot(List.of("a", "c"));
        assertThat(root1).isNotEqualTo(root2);
    }

    @Test
    @DisplayName("Should generate valid inclusion proof for any leaf")
    void testProofGeneration() {
        List<String> commitments = List.of("c0", "c1", "c2", "c3");
        String root = merkleTreeEngine.computeRoot(commitments);

        for (int i = 0; i < commitments.size(); i++) {
            MerkleTreeEngine.MerkleProof proof = merkleTreeEngine.generateProof(commitments, i);
            assertThat(proof.merkleRoot()).isEqualTo(root);
            assertThat(merkleTreeEngine.verifyProof(proof, commitments.get(i), root)).isTrue();
        }
    }

    @Test
    @DisplayName("Tampered commitment should fail proof verification")
    void testTamperedCommitmentFailsVerification() {
        List<String> commitments = List.of("c0", "c1", "c2", "c3");
        String root = merkleTreeEngine.computeRoot(commitments);
        MerkleTreeEngine.MerkleProof proof = merkleTreeEngine.generateProof(commitments, 2);

        // Verify with wrong commitment
        assertThat(merkleTreeEngine.verifyProof(proof, "TAMPERED", root)).isFalse();
    }

    @Test
    @DisplayName("Single leaf tree should have root equal to leaf hash")
    void testSingleLeafTree() {
        List<String> commitments = List.of("only-commitment");
        String root = merkleTreeEngine.computeRoot(commitments);
        assertThat(root).isNotNull().matches("[a-f0-9]{64}");
    }

    @Test
    @DisplayName("Empty commitment list should throw exception")
    void testEmptyListThrows() {
        assertThatThrownBy(() -> merkleTreeEngine.computeRoot(List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
