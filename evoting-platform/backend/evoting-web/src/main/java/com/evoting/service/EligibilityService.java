package com.evoting.service;

import com.evoting.core.domain.enums.CredentialStatus;
import com.evoting.core.domain.enums.EligibilityStatus;
import com.evoting.core.exception.CredentialException;
import com.evoting.entity.Credential;
import com.evoting.entity.EligibilityRecord;
import com.evoting.entity.Election;
import com.evoting.entity.VoterProfile;
import com.evoting.repository.CredentialRepository;
import com.evoting.repository.EligibilityRecordRepository;
import com.evoting.repository.ElectionRepository;
import com.evoting.repository.VoterProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Eligibility Authority service.
 *
 * TRUST BOUNDARY:
 * - ONLY this service knows the mapping: voter_reference -> user_id
 * - It issues ANONYMOUS credentials that the ballot gateway accepts
 * - The ballot gateway never learns voter identity from the credential
 *
 * FLOW:
 * 1. EA verifies voter is eligible for election (via eligibility_records)
 * 2. EA generates a random credential secret
 * 3. EA stores SHA-256(credential_secret) in the credentials table
 * 4. EA returns the plaintext credential_secret to the voter (ONCE ONLY)
 * 5. Voter uses credential_secret to generate nullifier and submit ballot
 */
@Service
@Transactional
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EligibilityRecordRepository eligibilityRecordRepository;
    private final CredentialRepository credentialRepository;
    private final ElectionRepository electionRepository;
    private final VoterProfileRepository voterProfileRepository;

    public EligibilityService(
            EligibilityRecordRepository eligibilityRecordRepository,
            CredentialRepository credentialRepository,
            ElectionRepository electionRepository,
            VoterProfileRepository voterProfileRepository) {
        this.eligibilityRecordRepository = eligibilityRecordRepository;
        this.credentialRepository = credentialRepository;
        this.electionRepository = electionRepository;
        this.voterProfileRepository = voterProfileRepository;
    }

    /**
     * Register a voter as eligible for an election.
     * Only ELIGIBILITY_AUTHORITY role may call this.
     *
     * @param userId     Internal user ID (EA knows this)
     * @param electionId Election to register for
     */
    @PreAuthorize("hasRole('ELIGIBILITY_AUTHORITY')")
    public void registerEligibility(UUID userId, UUID electionId) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new NoSuchElementException("Election not found: " + electionId));

        VoterProfile profile = voterProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new NoSuchElementException("Voter profile not found for user: " + userId));

        if (eligibilityRecordRepository.findByElectionIdAndVoterReference(
                electionId, profile.getVoterReference()).isPresent()) {
            throw new IllegalStateException("Voter already registered for this election");
        }

        EligibilityRecord record = new EligibilityRecord();
        record.setElection(election);
        record.setVoterReference(profile.getVoterReference());
        record.setStatus(EligibilityStatus.ELIGIBLE);
        record.setVerifiedAt(Instant.now());
        eligibilityRecordRepository.save(record);

        log.info("Voter registered as eligible: voterRef={}, electionId={}",
            maskRef(profile.getVoterReference()), electionId);
    }

    /**
     * Issue an anonymous credential to an eligible voter.
     *
     * SECURITY:
     * - Returns the plaintext credential token ONCE
     * - Stores only SHA-256(token) in the database
     * - The credential reference stored is opaque
     *
     * @param userId     Internal user ID
     * @param electionId Election to issue credential for
     * @return Plaintext credential token (one-time; voter must save it)
     */
    @PreAuthorize("hasAnyRole('VOTER','ELIGIBILITY_AUTHORITY')")
    public String issueCredential(UUID userId, UUID electionId) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new NoSuchElementException("Election not found: " + electionId));

        VoterProfile profile = voterProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new NoSuchElementException("Voter profile not found"));

        // Check eligibility
        boolean isEligible = eligibilityRecordRepository
            .existsByElectionIdAndVoterReferenceAndStatus(
                electionId, profile.getVoterReference(), EligibilityStatus.ELIGIBLE);
        if (!isEligible) {
            throw new CredentialException("Voter is not eligible for this election");
        }

        // Check if credential already issued
        if (credentialRepository.existsByElectionIdAndVoterReference(
                electionId, profile.getVoterReference())) {
            throw new CredentialException("Credential already issued for this voter and election");
        }

        // Generate cryptographically secure random token (256 bits)
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String credentialToken = HexFormat.of().formatHex(tokenBytes);

        // Store only the hash — never store plaintext credential
        String tokenHash = sha256(credentialToken);
        String credentialReference = UUID.randomUUID().toString().replace("-", "");

        Credential credential = new Credential();
        credential.setElection(election);
        credential.setVoterReference(profile.getVoterReference()); // opaque, not user_id
        credential.setCredentialReference(credentialReference);
        credential.setCredentialTokenHash(tokenHash);
        credential.setStatus(CredentialStatus.ISSUED);
        credential.setIssuedAt(Instant.now());
        // Credentials expire when election closes
        if (election.getEndTime() != null) {
            credential.setExpiresAt(election.getEndTime());
        }
        credentialRepository.save(credential);

        log.info("Credential issued: credentialRef={}, electionId={}",
            maskRef(credentialReference), electionId);

        // Return the PLAINTEXT token once — voter must save it securely
        return credentialToken;
    }

    /**
     * Check voter eligibility status.
     */
    @Transactional(readOnly = true)
    public EligibilityStatus getEligibilityStatus(UUID userId, UUID electionId) {
        VoterProfile profile = voterProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return EligibilityStatus.INELIGIBLE;

        return eligibilityRecordRepository
            .findByElectionIdAndVoterReference(electionId, profile.getVoterReference())
            .map(EligibilityRecord::getStatus)
            .orElse(EligibilityStatus.INELIGIBLE);
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                digest.digest(input.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Log only first 8 chars of sensitive references */
    private String maskRef(String ref) {
        if (ref == null || ref.length() < 8) return "***";
        return ref.substring(0, 8) + "...";
    }
}
