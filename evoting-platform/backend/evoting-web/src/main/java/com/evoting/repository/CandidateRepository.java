package com.evoting.repository;

import com.evoting.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
    List<Candidate> findByElectionIdOrderByBallotOrderAsc(UUID electionId);
    Optional<Candidate> findByElectionIdAndCandidateCode(UUID electionId, String candidateCode);
    boolean existsByElectionIdAndCandidateCode(UUID electionId, String candidateCode);
}
