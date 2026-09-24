package com.evoting.repository;

import com.evoting.entity.BallotMetadata;
import com.evoting.core.domain.enums.BallotState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BallotMetadataRepository extends JpaRepository<BallotMetadata, UUID> {

    Optional<BallotMetadata> findByBallotReference(String ballotReference);
    Optional<BallotMetadata> findByCommitment(String commitment);
    Optional<BallotMetadata> findByIdempotencyKey(String idempotencyKey);

    long countByElectionId(UUID electionId);
    long countByElectionIdAndLifecycleState(UUID electionId, BallotState state);

    @Query("SELECT b FROM BallotMetadata b WHERE b.election.id = :electionId AND b.batch IS NULL AND b.lifecycleState = 'BALLOT_ACCEPTED' ORDER BY b.acceptedAt ASC")
    List<BallotMetadata> findUnbatchedByElectionId(@Param("electionId") UUID electionId);

    List<BallotMetadata> findByBatchId(UUID batchId);
}
