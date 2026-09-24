package com.evoting.service;

import com.evoting.dto.response.AuditResponse;
import com.evoting.entity.AuditAnchor;
import com.evoting.entity.Election;
import com.evoting.entity.MerkleBatch;
import com.evoting.repository.AuditAnchorRepository;
import com.evoting.repository.BallotMetadataRepository;
import com.evoting.repository.ElectionRepository;
import com.evoting.repository.MerkleBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public audit service.
 * Returns election audit data for independent verification.
 * All operations are read-only and expose only non-identifying information.
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    private final ElectionRepository electionRepository;
    private final MerkleBatchRepository merkleBatchRepository;
    private final BallotMetadataRepository ballotMetadataRepository;
    private final AuditAnchorRepository auditAnchorRepository;

    public AuditService(
            ElectionRepository electionRepository,
            MerkleBatchRepository merkleBatchRepository,
            BallotMetadataRepository ballotMetadataRepository,
            AuditAnchorRepository auditAnchorRepository) {
        this.electionRepository = electionRepository;
        this.merkleBatchRepository = merkleBatchRepository;
        this.ballotMetadataRepository = ballotMetadataRepository;
        this.auditAnchorRepository = auditAnchorRepository;
    }

    public AuditResponse getAuditReport(UUID electionId) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new NoSuchElementException("Election not found: " + electionId));

        List<MerkleBatch> batches = merkleBatchRepository.findByElectionIdOrderByCreatedAtAsc(electionId);
        List<AuditAnchor> anchors = auditAnchorRepository.findByElectionIdOrderByCreatedAtDesc(electionId);
        long totalBallots = ballotMetadataRepository.countByElectionId(electionId);
        boolean mismatchDetected = anchors.stream().anyMatch(AuditAnchor::isMismatchDetected);

        List<AuditResponse.BatchSummary> batchSummaries = batches.stream()
            .map(b -> AuditResponse.BatchSummary.builder()
                .batchReference(b.getBatchReference())
                .merkleRoot(b.getMerkleRoot())
                .leafCount(b.getLeafCount())
                .blockchainTxId(b.getBlockchainTxId())
                .status(b.getStatus().name())
                .anchoredAt(b.getAnchoredAt())
                .build())
            .collect(Collectors.toList());

        List<AuditResponse.AnchorSummary> anchorSummaries = anchors.stream()
            .map(a -> AuditResponse.AnchorSummary.builder()
                .anchorReference(a.getAnchorReference())
                .auditDigest(a.getAuditDigest())
                .blockchainTxId(a.getBlockchainTxId())
                .verificationStatus(a.getVerificationStatus())
                .mismatchDetected(a.isMismatchDetected())
                .createdAt(a.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return AuditResponse.builder()
            .electionId(election.getId().toString())
            .electionCode(election.getElectionCode())
            .title(election.getTitle())
            .state(election.getState())
            .totalBallotsAccepted(totalBallots)
            .totalBatchesAnchored(batches.size())
            .batches(batchSummaries)
            .auditAnchors(anchorSummaries)
            .auditMismatchDetected(mismatchDetected)
            .build();
    }

    public List<AuditResponse> getAllAuditReports() {
        return electionRepository.findAll().stream()
            .map(e -> getAuditReport(e.getId()))
            .collect(Collectors.toList());
    }
}
