package com.evoting.repository;

import com.evoting.entity.Nullifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NullifierRepository extends JpaRepository<Nullifier, UUID> {

    /**
     * Check if a nullifier has already been used for this election.
     * This is the primary double-vote prevention check.
     */
    boolean existsByElectionIdAndNullifierHash(UUID electionId, String nullifierHash);

    Optional<Nullifier> findByElectionIdAndNullifierHash(UUID electionId, String nullifierHash);

    long countByElectionId(UUID electionId);
}
