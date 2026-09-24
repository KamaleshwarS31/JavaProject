package com.evoting.repository;

import com.evoting.entity.MerkleBatch;
import com.evoting.core.domain.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerkleBatchRepository extends JpaRepository<MerkleBatch, UUID> {
    Optional<MerkleBatch> findByBatchReference(String batchReference);
    List<MerkleBatch> findByElectionIdOrderByCreatedAtAsc(UUID electionId);
    Optional<MerkleBatch> findFirstByElectionIdAndStatusOrderByCreatedAtAsc(UUID electionId, BatchStatus status);
    long countByElectionId(UUID electionId);
}
