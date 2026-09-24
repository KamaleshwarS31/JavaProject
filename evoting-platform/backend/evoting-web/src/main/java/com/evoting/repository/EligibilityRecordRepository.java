package com.evoting.repository;

import com.evoting.entity.EligibilityRecord;
import com.evoting.core.domain.enums.EligibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EligibilityRecordRepository extends JpaRepository<EligibilityRecord, UUID> {
    Optional<EligibilityRecord> findByElectionIdAndVoterReference(UUID electionId, String voterReference);
    boolean existsByElectionIdAndVoterReferenceAndStatus(UUID electionId, String voterReference, EligibilityStatus status);
}
