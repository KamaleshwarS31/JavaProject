package com.evoting.repository;

import com.evoting.entity.VoterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** RESTRICTED: Only Eligibility Authority service should call these methods. */
@Repository
public interface VoterProfileRepository extends JpaRepository<VoterProfile, UUID> {
    Optional<VoterProfile> findByUserId(UUID userId);
    Optional<VoterProfile> findByVoterReference(String voterReference);
    boolean existsByUserId(UUID userId);
}
