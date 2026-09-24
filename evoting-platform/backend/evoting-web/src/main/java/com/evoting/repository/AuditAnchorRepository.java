package com.evoting.repository;

import com.evoting.entity.AuditAnchor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditAnchorRepository extends JpaRepository<AuditAnchor, UUID> {
    List<AuditAnchor> findByElectionIdOrderByCreatedAtDesc(UUID electionId);
    Optional<AuditAnchor> findByAnchorReference(String anchorReference);
    List<AuditAnchor> findByMismatchDetectedTrue();
}
