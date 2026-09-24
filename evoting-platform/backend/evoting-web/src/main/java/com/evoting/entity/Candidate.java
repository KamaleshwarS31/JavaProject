package com.evoting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidates", schema = "public",
    uniqueConstraints = { @UniqueConstraint(name = "uq_candidate_code", columnNames = {"election_id", "candidate_code"}) })
@Getter @Setter @NoArgsConstructor
public class Candidate {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false, foreignKey = @ForeignKey(name = "fk_candidate_election"))
    private Election election;

    @NotBlank @Size(max = 20)
    @Column(name = "candidate_code", nullable = false, length = 20)
    private String candidateCode;

    @NotBlank @Size(max = 255)
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @Column(name = "party_name", length = 100)
    private String partyName;

    @Column(name = "symbol_uri", length = 500)
    private String symbolUri;

    @Column(name = "ballot_order", nullable = false)
    private int ballotOrder = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
