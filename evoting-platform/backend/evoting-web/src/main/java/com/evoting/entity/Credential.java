package com.evoting.entity;

import com.evoting.core.domain.enums.CredentialStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credentials", schema = "public",
    uniqueConstraints = { @UniqueConstraint(name = "uq_credential_reference", columnNames = "credential_reference") })
@Getter @Setter @NoArgsConstructor
public class Credential {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false, foreignKey = @ForeignKey(name = "fk_credential_election"))
    private Election election;

    @Column(name = "voter_reference", nullable = false, length = 64)
    private String voterReference;

    @Column(name = "credential_reference", nullable = false, unique = true, length = 64)
    private String credentialReference;

    @Column(name = "credential_token_hash", nullable = false, length = 64)
    private String credentialTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CredentialStatus status = CredentialStatus.ISSUED;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
