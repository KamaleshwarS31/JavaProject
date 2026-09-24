package com.evoting.service;

import com.evoting.core.domain.enums.ElectionState;
import com.evoting.core.crypto.BallotEncryptionService;
import com.evoting.dto.request.CandidateRequest;
import com.evoting.dto.request.ElectionCreateRequest;
import com.evoting.dto.response.CandidateResponse;
import com.evoting.dto.response.ElectionResponse;
import com.evoting.entity.Candidate;
import com.evoting.entity.Election;
import com.evoting.repository.BallotMetadataRepository;
import com.evoting.repository.CandidateRepository;
import com.evoting.repository.ElectionRepository;
import com.evoting.repository.MerkleBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Election management service.
 * All business logic here — controllers only delegate.
 */
@Service
@Transactional
public class ElectionService {

    private static final Logger log = LoggerFactory.getLogger(ElectionService.class);

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final BallotMetadataRepository ballotMetadataRepository;
    private final MerkleBatchRepository merkleBatchRepository;
    private final BallotEncryptionService ballotEncryptionService;

    public ElectionService(
            ElectionRepository electionRepository,
            CandidateRepository candidateRepository,
            BallotMetadataRepository ballotMetadataRepository,
            MerkleBatchRepository merkleBatchRepository,
            BallotEncryptionService ballotEncryptionService) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.ballotMetadataRepository = ballotMetadataRepository;
        this.merkleBatchRepository = merkleBatchRepository;
        this.ballotEncryptionService = ballotEncryptionService;
    }

    @PreAuthorize("hasRole('ELECTION_ADMIN')")
    public ElectionResponse createElection(ElectionCreateRequest request) throws Exception {
        if (electionRepository.existsByElectionCode(request.getElectionCode())) {
            throw new IllegalArgumentException("Election code already exists: " + request.getElectionCode());
        }

        // Generate RSA-4096 key pair for this election
        KeyPair keyPair = ballotEncryptionService.generateElectionKeyPair();
        String publicKeyBase64 = ballotEncryptionService.exportPublicKeyBase64(keyPair.getPublic());
        // TODO: Private key should be split via SSS and distributed to key holders
        // For this prototype, we log a warning that the private key needs external handling
        log.warn("[KEY MANAGEMENT] Election {} RSA private key generated. In production: split via SSS and distribute to 3 key holders. This prototype does not handle private key distribution.", request.getElectionCode());

        Election election = new Election();
        election.setElectionCode(request.getElectionCode());
        election.setTitle(request.getTitle());
        election.setDescription(request.getDescription());
        election.setElectionType(request.getElectionType());
        election.setPublicKeyPem(publicKeyBase64);
        election.setStartTime(request.getStartTime());
        election.setEndTime(request.getEndTime());
        election.setBatchSizeLimit(request.getBatchSizeLimit());
        election.setState(ElectionState.DRAFT);

        election = electionRepository.save(election);
        log.info("Election created: code={}, id={}", election.getElectionCode(), election.getId());

        return toElectionResponse(election, true);
    }

    @Transactional(readOnly = true)
    public ElectionResponse getElection(UUID electionId) {
        Election election = findElectionById(electionId);
        return toElectionResponse(election, true);
    }

    @Transactional(readOnly = true)
    public List<ElectionResponse> getAllElections() {
        return electionRepository.findAll().stream()
            .map(e -> toElectionResponse(e, false))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ElectionResponse> getActiveElections() {
        return electionRepository.findActiveElections().stream()
            .map(e -> toElectionResponse(e, false))
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ELECTION_ADMIN')")
    public ElectionResponse transitionState(UUID electionId, ElectionState targetState) {
        Election election = findElectionById(electionId);
        election.transitionTo(targetState); // Throws ElectionStateException if invalid
        election = electionRepository.save(election);
        log.info("Election state transitioned: id={}, newState={}", electionId, targetState);
        return toElectionResponse(election, true);
    }

    @PreAuthorize("hasRole('ELECTION_ADMIN')")
    public CandidateResponse addCandidate(UUID electionId, CandidateRequest request) {
        Election election = findElectionById(electionId);
        if (!election.getState().isConfigurationEditable()) {
            throw new IllegalStateException("Cannot add candidates after election leaves DRAFT state");
        }
        if (candidateRepository.existsByElectionIdAndCandidateCode(electionId, request.getCandidateCode())) {
            throw new IllegalArgumentException("Candidate code already exists: " + request.getCandidateCode());
        }

        Candidate candidate = new Candidate();
        candidate.setElection(election);
        candidate.setCandidateCode(request.getCandidateCode());
        candidate.setDisplayName(request.getDisplayName());
        candidate.setBiography(request.getBiography());
        candidate.setPartyName(request.getPartyName());
        candidate.setSymbolUri(request.getSymbolUri());
        candidate.setBallotOrder(request.getBallotOrder());
        candidate = candidateRepository.save(candidate);

        return toCandidateResponse(candidate);
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> getCandidates(UUID electionId) {
        findElectionById(electionId); // Validate election exists
        return candidateRepository.findByElectionIdOrderByBallotOrderAsc(electionId)
            .stream().map(this::toCandidateResponse).collect(Collectors.toList());
    }

    // === Private helpers ===

    private Election findElectionById(UUID id) {
        return electionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Election not found: " + id));
    }

    private ElectionResponse toElectionResponse(Election e, boolean includeCandidates) {
        long totalBallots = ballotMetadataRepository.countByElectionId(e.getId());
        long totalBatches = merkleBatchRepository.countByElectionId(e.getId());

        ElectionResponse.ElectionResponseBuilder builder = ElectionResponse.builder()
            .id(e.getId().toString())
            .electionCode(e.getElectionCode())
            .title(e.getTitle())
            .description(e.getDescription())
            .state(e.getState())
            .electionType(e.getElectionType())
            .publicKeyPem(e.getPublicKeyPem())
            .startTime(e.getStartTime())
            .endTime(e.getEndTime())
            .votingOpen(e.isVotingOpen())
            .totalBallotsAccepted(totalBallots)
            .totalBatchesAnchored(totalBatches)
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt());

        if (includeCandidates) {
            builder.candidates(
                candidateRepository.findByElectionIdOrderByBallotOrderAsc(e.getId())
                    .stream().map(this::toCandidateResponse).collect(Collectors.toList())
            );
        }

        return builder.build();
    }

    private CandidateResponse toCandidateResponse(Candidate c) {
        return CandidateResponse.builder()
            .id(c.getId().toString())
            .candidateCode(c.getCandidateCode())
            .displayName(c.getDisplayName())
            .biography(c.getBiography())
            .partyName(c.getPartyName())
            .symbolUri(c.getSymbolUri())
            .ballotOrder(c.getBallotOrder())
            .status(c.getStatus())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
