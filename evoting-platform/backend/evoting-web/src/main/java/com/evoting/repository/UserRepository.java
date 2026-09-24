package com.evoting.repository;

import com.evoting.entity.User;
import com.evoting.core.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * Security: All custom queries use named parameters (no string concatenation).
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1, u.updatedAt = :now WHERE u.id = :id")
    void incrementFailedLoginAttempts(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null, u.updatedAt = :now WHERE u.id = :id")
    void resetFailedLoginAttempts(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil, u.updatedAt = :now WHERE u.id = :id")
    void lockAccount(@Param("id") UUID id, @Param("lockedUntil") Instant lockedUntil, @Param("now") Instant now);
}
