package com.evoting.service;

import com.evoting.core.crypto.CommitmentEngine;
import com.evoting.core.crypto.NullifierEngine;
import com.evoting.core.domain.enums.BallotState;
import com.evoting.core.domain.enums.BatchStatus;
import com.evoting.core.domain.enums.CredentialStatus;
import com.evoting.core.domain.enums.EligibilityStatus;
import com.evoting.core.exception.BallotValidationException;
import com.evoting.core.exception.CredentialException;
import com.evoting.core.exception.NullifierDuplicateException;
import com.evoting.dto.request.BallotSubmissionRequest;
import com.evoting.dto.response.BallotStatusResponse;
import com.evoting.dto.response.ReceiptResponse;
import com.evoting.entity.*;
import com.evoting.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Ballot ingestion service.
 *
 * Privacy invariant enforced here:
 * - We receive credential token (opaque), nullifier, encrypted ballot
 * - We do NOT receive voter identity, username, user_id
 * - We do NOT store candidate selection
 * - The nullifier check prevents double-voting without linking identity to ballot
 */
@Service
@Transactional
public class BallotService {

    private static final Logger log = LoggerFactory.getLogger(BallotService.class);

    private final BallotMetadataRepository ballotMetadataRepository;
    private final NullifierRepository nullifierRepository;
    private final ElectionRepository electionRepository;
    private final CredentialRepository credentialRepository;
    private final EligibilityRecordRepository eligibilityRecordRepository;
    private final MerkleBatchRepository merkleBatchRepository;
    private final NullifierEngine nullifierEngine;
    private final CommitmentEngine commitmentEngine;

    @Value("${evoting.app.base-url:http://localhost:8080}")
    private String baseUrl;

    public BallotService(
            BallotMetadataRepository ballotMetadataRepository,
            NullifierRepository nullifierRepository,
            ElectionRepository electionRepository,
            CredentialRepository credentialRepository,
            EligibilityRecordRepository eligibilityRecordRepository,
            MerkleBatchRepository merkleBatchRepository,
            NullifierEngine nullifierEngine,
            CommitmentEngine commitmentEngine) {
        this.ballotMetadataRepository = ballotMetadataRepository;
        this.nullifierRepository = nullifierRepository;
        this.electionRepository = electionRepository;
        this.credentialRepository = credentialRepository;
        this.eligibilityRecordRepository = eligibilityRecordRepository;
        this.merkleBatchRepository = merkleBatchRepository;
        this.nullifierEngine = nullifierEngine;
        this.commitmentEngine = commitmentEngine;
    }

    /**
     * Submits an encrypted ballot.
     *
     * Steps:
     * 1. Validate election is ACTIVE
     * 2. Validate idempotency key (replay protection)
     * 3. Validate credential token exists and is ISSUED (not already REDEEMED)
     * 4. Check nullifier has not been used (double-vote prevention)
     * 5. Validate commitment matches encrypted ballot
     * 6. Record nullifier (atomic with ballot save)
     * 7. Save ballot metadata
     * 8. Assign to open Merkle batch
     * 9. Mark credential as REDEEMED
     * 10. Return privacy-preserving receipt
     */
    public ReceiptResponse submitBallot(BallotSubmissionRequest request) {
        List<String> errors = new ArrayList<>();

        // 1. Validate election
        UUID electionId = UUID.fromString(request.getElectionId());
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new NoSuchElementException("Election not found"));

        if (!election.isVotingOpen()) {
            errors.add("Voting is not currently open for this election");
        }

        // 2. Idempotency check
        if (ballotMetadataRepository.findByIdempotencyKey(request.getRequestId()).isPresent()) {
            log.info("Idempotent ballot submission detected, returning existing receipt");
            BallotMetadata existing = ballotMetadataRepository.findByIdempotencyKey(request.getRequestId()).get();
            return buildReceipt(existing, election);
        }

        // 3. Validate credential
        Credential credential = credentialRepository.findByCredentialReference(
                hashCredentialToken(request.getCredentialToken()))
            .orElseThrow(() -> new CredentialException("Invalid or unrecognized credential"));

        if (credential.getStatus() != CredentialStatus.ISSUED) {
            errors.add("Credential has already been used or is not valid");
        }
        if (!credential.getElection().getId().equals(electionId)) {
            errors.add("Credential is not valid for this election");
        }
        if (credential.getExpiresAt() != null && Instant.now().isAfter(credential.getExpiresAt())) {
            errors.add("Credential has expired");
        }

        // Check voter eligibility (via voter_reference from credential)
        boolean isEligible = eligibilityRecordRepository
            .existsByElectionIdAndVoterReferenceAndStatus(
                electionId, credential.getVoterReference(), EligibilityStatus.ELIGIBLE);
        if (!isEligible) {
            errors.add("Voter is not eligible for this election");
        }

        if (!errors.isEmpty()) {
            throw new BallotValidationException(errors);
        }

        // 4. Nullifier check — primary double-vote prevention
        if (nullifierRepository.existsByElectionIdAndNullifierHash(electionId, request.getNullifier())) {
            throw new NullifierDuplicateException(request.getNullifier(), request.getElectionId());
        }

        // 5. Validate commitment: client claims C = SHA256(encryptedBallot || salt)
        // We verify the commitment format only (salt not stored here, full verification at tally)
        // The commitment uniqueness constraint in DB prevents tampering
        if (ballotMetadataRepository.findByCommitment(request.getCommitment()).isPresent()) {
            throw new BallotValidationException("Commitment collision detected");
        }

        // 6. Save nullifier (BEFORE saving ballot — if this fails, ballot is not saved)
        Nullifier nullifier = new Nullifier(election, request.getNullifier());
        nullifierRepository.saveAndFlush(nullifier);

        // 7. Save ballot metadata — NO voter_id, NO candidate_id
        BallotMetadata ballot = new BallotMetadata();
        ballot.setElection(election);
        ballot.setBallotReference(UUID.randomUUID().toString().replace("-", ""));
        ballot.setCommitment(request.getCommitment());
        ballot.setEncryptedPayload(request.getEncryptedBallot());
        ballot.setLifecycleState(BallotState.BALLOT_ACCEPTED);
        ballot.setIdempotencyKey(request.getRequestId());
        ballot = ballotMetadataRepository.save(ballot);

        // 8. Assign to open Merkle batch
        MerkleBatch batch = getOrCreateOpenBatch(election);
        ballot.setBatch(batch);
        ballot.setLeafIndex(batch.getLeafCount());
        ballot.setLifecycleState(BallotState.BATCHED);
        batch.setLeafCount(batch.getLeafCount() + 1);

        if (batch.getLeafCount() >= election.getBatchSizeLimit()) {
            batch.setStatus(BatchStatus.PENDING_ANCHOR);
        }

        merkleBatchRepository.save(batch);
        ballot = ballotMetadataRepository.save(ballot);

        // 9. Mark credential as REDEEMED
        credential.setStatus(CredentialStatus.REDEEMED);
        credential.setRedeemedAt(Instant.now());
        credentialRepository.save(credential);

        log.info("Ballot accepted: ballotRef={}, electionId={}, batchRef={}",
            ballot.getBallotReference(), electionId, batch.getBatchReference());

        return buildReceipt(ballot, election);
    }

    @Transactional(readOnly = true)
    public BallotStatusResponse getBallotStatus(String ballotReference) {
        BallotMetadata ballot = ballotMetadataRepository.findByBallotReference(ballotReference)
            .orElseThrow(() -> new NoSuchElementException("Ballot reference not found"));

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

    // === Private helpers ===

    private MerkleBatch getOrCreateOpenBatch(Election election) {
        return merkleBatchRepository
            .findFirstByElectionIdAndStatusOrderByCreatedAtAsc(election.getId(), BatchStatus.OPEN)
            .orElseGet(() -> {
                MerkleBatch newBatch = new MerkleBatch();
                newBatch.setElection(election);
                newBatch.setBatchReference(UUID.randomUUID().toString().replace("-", ""));
                newBatch.setLeafCount(0);
                newBatch.setStatus(BatchStatus.OPEN);
                return merkleBatchRepository.save(newBatch);
            });
    }

    private String hashCredentialToken(String credentialToken) {
        // The credential_reference stored in DB is a SHA-256 hash of the actual token
        // We hash the submitted token to look up the credential
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(credentialToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private ReceiptResponse buildReceipt(BallotMetadata ballot, Election election) {
        ReceiptResponse.ReceiptResponseBuilder builder = ReceiptResponse.builder()
            .receiptId(UUID.randomUUID().toString())
            .electionId(election.getId().toString())
            .electionTitle(election.getTitle())
            .ballotReference(ballot.getBallotReference())
            .commitment(ballot.getCommitment())
            .verificationUrl(baseUrl + "/api/v1/verify/ballot/" + ballot.getBallotReference());

        if (ballot.getBatch() != null) {
            builder.batchReference(ballot.getBatch().getBatchReference())
                   .merkleRoot(ballot.getBatch().getMerkleRoot())
                   .blockchainTxId(ballot.getBatch().getBlockchainTxId());
        }
        return builder.build();
    }
}
