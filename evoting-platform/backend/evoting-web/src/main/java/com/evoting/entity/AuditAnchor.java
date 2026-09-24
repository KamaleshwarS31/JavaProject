package com.evoting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_anchors", schema = "public",
    uniqueConstraints = { @UniqueConstraint(name = "uq_anchor_reference", columnNames = "anchor_reference") })
@Getter @Setter @NoArgsConstructor
public class AuditAnchor {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false, foreignKey = @ForeignKey(name = "fk_anchor_election"))
    private Election election;

    @Column(name = "anchor_reference", nullable = false, unique = true, length = 64)
    private String anchorReference;

    @Column(name = "audit_digest", nullable = false, length = 64)
    private String auditDigest;

    @Column(name = "batch_range", length = 255)
    private String batchRange;

    @Column(name = "blockchain_tx_id", length = 100)
    private String blockchainTxId;

    @Column(name = "verification_status", length = 30)
    private String verificationStatus = "PENDING";

    @Column(name = "mismatch_detected", nullable = false)
    private boolean mismatchDetected = false;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
