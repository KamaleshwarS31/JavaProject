package com.evoting.service;

import com.evoting.core.crypto.MerkleTreeEngine;
import com.evoting.dto.response.BallotStatusResponse;
import com.evoting.dto.response.MerkleProofResponse;
import com.evoting.entity.BallotMetadata;
import com.evoting.entity.MerkleBatch;
import com.evoting.repository.BallotMetadataRepository;
import com.evoting.repository.MerkleBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Public verification service — ballot inclusion, Merkle proofs.
 * All operations are read-only and publicly accessible.
 */
@Service
@Transactional(readOnly = true)
public class VerificationService {

    private final BallotMetadataRepository ballotMetadataRepository;
    private final MerkleBatchRepository merkleBatchRepository;
    private final MerkleTreeEngine merkleTreeEngine;

    public VerificationService(
            BallotMetadataRepository ballotMetadataRepository,
            MerkleBatchRepository merkleBatchRepository,
            MerkleTreeEngine merkleTreeEngine) {
        this.ballotMetadataRepository = ballotMetadataRepository;
        this.merkleBatchRepository = merkleBatchRepository;
        this.merkleTreeEngine = merkleTreeEngine;
    }

    /**
     * Generates a Merkle inclusion proof for the voter's ballot commitment.
     * The voter can independently verify their ballot is included in the published batch
     * without revealing their identity or vote choice.
     */
    public MerkleProofResponse getMerkleProof(String ballotReference) {
        BallotMetadata ballot = ballotMetadataRepository.findByBallotReference(ballotReference)
            .orElseThrow(() -> new NoSuchElementException("Ballot reference not found"));

        if (ballot.getBatch() == null || ballot.getBatch().getMerkleRoot() == null) {
            return MerkleProofResponse.builder()
                .commitment(ballot.getCommitment())
                .verified(false)
                .verificationMessage("Ballot not yet anchored in a Merkle batch. Please check again later.")
                .build();
        }

        MerkleBatch batch = ballot.getBatch();

        // Reconstruct the batch's commitment list in leaf_index order
        List<String> batchCommitments = ballotMetadataRepository.findByBatchId(batch.getId())
            .stream()
            .sorted(Comparator.comparingInt(b -> b.getLeafIndex() != null ? b.getLeafIndex() : 0))
            .map(BallotMetadata::getCommitment)
            .collect(Collectors.toList());

        if (ballot.getLeafIndex() == null || ballot.getLeafIndex() >= batchCommitments.size()) {
            return MerkleProofResponse.builder()
                .commitment(ballot.getCommitment())
                .verified(false)
                .verificationMessage("Proof generation error: leaf index invalid.")
                .build();
        }

        MerkleTreeEngine.MerkleProof proof = merkleTreeEngine.generateProof(batchCommitments, ballot.getLeafIndex());
        boolean isValid = merkleTreeEngine.verifyProof(proof, ballot.getCommitment(), batch.getMerkleRoot());

        List<MerkleProofResponse.ProofElementDto> proofElements = proof.proof().stream()
            .map(e -> MerkleProofResponse.ProofElementDto.builder()
                .siblingHash(e.siblingHash())
                .isLeft(e.isLeft())
                .build())
            .collect(Collectors.toList());

        return MerkleProofResponse.builder()
            .commitment(ballot.getCommitment())
            .merkleRoot(batch.getMerkleRoot())
            .batchReference(batch.getBatchReference())
            .blockchainTxId(batch.getBlockchainTxId())
            .leafIndex(ballot.getLeafIndex())
            .proof(proofElements)
            .verified(isValid)
            .verificationMessage(isValid ? "Ballot commitment is included in the published Merkle tree." : "Verification failed.")
            .build();
    }

    public BallotStatusResponse verifyByCommitment(String commitment) {
        BallotMetadata ballot = ballotMetadataRepository.findByCommitment(commitment)
            .orElseThrow(() -> new NoSuchElementException("Commitment not found in the system"));

        BallotStatusResponse.BallotStatusResponseBuilder builder = BallotStatusResponse.builder()
            .ballotReference(ballot.getBallotReference())
            .electionId(ballot.getElection().getId().toString())
            .lifecycleState(ballot.getLifecycleState())
            .commitment(ballot.getCommitment())
            .acceptedAt(ballot.getAcceptedAt())
            .anchoredAt(ballot.getAnchoredAt());

        if (ballot.getBatch() != null) {
            builder.batchReference(ballot.getBatch().getBatchReference())
                   .merkleRoot(ballot.getBatch().getMerkleRoot())
                   .blockchainTxId(ballot.getBatch().getBlockchainTxId());
        }
        return builder.build();
    }
}
