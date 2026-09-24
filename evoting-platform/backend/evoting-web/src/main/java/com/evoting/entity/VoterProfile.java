package com.evoting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voter_profiles", schema = "public",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_voter_profile_user_id", columnNames = "user_id"),
        @UniqueConstraint(name = "uq_voter_reference", columnNames = "voter_reference")
    }
)
@Getter @Setter @NoArgsConstructor
public class VoterProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_voter_profile_user"))
    private User user;

    @Column(name = "voter_reference", nullable = false, unique = true, length = 64)
    private String voterReference;

    @Column(name = "national_id_hash", length = 64)
    private String nationalIdHash;

    @Column(name = "verification_status", nullable = false, length = 30)
    private String verificationStatus = "PENDING_VERIFICATION";

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Version @Column(name = "version")
    private Long version;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
