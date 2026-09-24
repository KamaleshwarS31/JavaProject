package com.evoting.repository;

import com.evoting.entity.Credential;
import com.evoting.core.domain.enums.CredentialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, UUID> {
    Optional<Credential> findByCredentialReference(String credentialReference);
    Optional<Credential> findByElectionIdAndVoterReference(UUID electionId, String voterReference);
    boolean existsByElectionIdAndVoterReference(UUID electionId, String voterReference);
}
