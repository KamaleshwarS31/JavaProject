package com.evoting.core.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HexFormat;
import java.util.stream.Collectors;

/**
 * Binary Merkle Tree implementation for ballot commitment batching.
 *
 * <p>Provides:
 * <ul>
 *   <li>Merkle root computation from a list of ballot commitments</li>
 *   <li>Merkle proof (inclusion proof) generation for any leaf</li>
 *   <li>Merkle proof verification</li>
 * </ul>
 * </p>
 *
 * <p>The Merkle root is anchored to Hyperledger Fabric for tamper evidence.
 * This allows any voter to independently verify their ballot commitment
 * is included in the published batch without revealing their identity or vote choice.</p>
 *
 * <p>Algorithm:
 * <pre>
 *   Leaf_i   = SHA256("LEAF:" + commitment_i)
 *   Parent   = SHA256(Left_child + Right_child)
 *   Root     = top-level parent hash
 * </pre>
 * </p>
 */
@Component
public class MerkleTreeEngine {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String LEAF_PREFIX = "EVOTING_LEAF_V1:";
    private static final String NODE_PREFIX = "EVOTING_NODE_V1:";

    /**
     * Represents a Merkle inclusion proof for a single leaf.
     *
     * @param leafHash    the hash of the commitment being proved
     * @param proof       ordered list of sibling hashes (left=true means sibling is on the left)
     * @param merkleRoot  the root hash this proof is valid against
     * @param leafIndex   zero-based index of the leaf in the batch
     */
    public record MerkleProof(
        String leafHash,
        List<ProofElement> proof,
        String merkleRoot,
        int leafIndex
    ) {}

    /** Single element in a Merkle proof path */
    public record ProofElement(String siblingHash, boolean isLeft) {}

    /**
     * Computes the Merkle root for a batch of ballot commitments.
     *
     * @param commitments ordered list of hex-encoded commitment strings
     * @return hex-encoded Merkle root
     * @throws IllegalArgumentException if commitments list is null or empty
     */
    public String computeRoot(List<String> commitments) {
        if (commitments == null || commitments.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute Merkle root from empty commitment list");
        }

        // Compute leaf hashes
        List<String> currentLevel = commitments.stream()
            .map(this::hashLeaf)
            .collect(Collectors.toCollection(ArrayList::new));

        // Build tree bottom-up
        while (currentLevel.size() > 1) {
            currentLevel = computeNextLevel(currentLevel);
        }

        return currentLevel.get(0);
    }

    /**
     * Generates a Merkle inclusion proof for a specific commitment.
     *
     * @param commitments all commitments in the batch (same order as when root was computed)
     * @param targetIndex zero-based index of the commitment to prove
     * @return MerkleProof for independent verification
     */
    public MerkleProof generateProof(List<String> commitments, int targetIndex) {
        if (commitments == null || commitments.isEmpty()) {
            throw new IllegalArgumentException("Commitment list must not be empty");
        }
        if (targetIndex < 0 || targetIndex >= commitments.size()) {
            throw new IndexOutOfBoundsException("Target index " + targetIndex + " out of bounds for list size " + commitments.size());
        }

        List<String> currentLevel = commitments.stream()
            .map(this::hashLeaf)
            .collect(Collectors.toCollection(ArrayList::new));

        String leafHash = currentLevel.get(targetIndex);
        String merkleRoot = computeRoot(commitments);

        List<ProofElement> proofPath = new ArrayList<>();
        int currentIndex = targetIndex;

        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left; // duplicate last if odd

                if (i == currentIndex || i + 1 == currentIndex) {
                    if (i == currentIndex) {
                        // Target is left child; sibling is right
                        proofPath.add(new ProofElement(right, false));
                    } else {
                        // Target is right child; sibling is left
                        proofPath.add(new ProofElement(left, true));
                    }
                }
                nextLevel.add(hashNodes(left, right));
            }
            currentIndex = currentIndex / 2;
            currentLevel = nextLevel;
        }

        return new MerkleProof(leafHash, Collections.unmodifiableList(proofPath), merkleRoot, targetIndex);
    }

    /**
     * Verifies a Merkle inclusion proof against a known root.
     *
     * @param proof    the Merkle proof to verify
     * @param commitment the original commitment (to recompute leaf hash)
     * @param expectedRoot the expected Merkle root (from blockchain)
     * @return true if the proof is valid
     */
    public boolean verifyProof(MerkleProof proof, String commitment, String expectedRoot) {
        String currentHash = hashLeaf(commitment);

        for (ProofElement element : proof.proof()) {
            if (element.isLeft()) {
                currentHash = hashNodes(element.siblingHash(), currentHash);
            } else {
                currentHash = hashNodes(currentHash, element.siblingHash());
            }
        }

        return MessageDigest.isEqual(
            currentHash.getBytes(StandardCharsets.UTF_8),
            expectedRoot.getBytes(StandardCharsets.UTF_8)
        );
    }

    // === Private helpers ===

    private String hashLeaf(String commitment) {
        return sha256(LEAF_PREFIX + commitment);
    }

    private String hashNodes(String left, String right) {
        return sha256(NODE_PREFIX + left + right);
    }

    private List<String> computeNextLevel(List<String> level) {
        List<String> nextLevel = new ArrayList<>();
        for (int i = 0; i < level.size(); i += 2) {
            String left = level.get(i);
            String right = (i + 1 < level.size()) ? level.get(i + 1) : left;
            nextLevel.add(hashNodes(left, right));
        }
        return nextLevel;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
