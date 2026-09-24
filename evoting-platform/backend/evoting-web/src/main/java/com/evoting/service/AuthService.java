package com.evoting.service;

import com.evoting.core.domain.enums.UserRole;
import com.evoting.dto.request.LoginRequest;
import com.evoting.dto.request.RegisterRequest;
import com.evoting.dto.response.AuthResponse;
import com.evoting.entity.User;
import com.evoting.entity.VoterProfile;
import com.evoting.repository.UserRepository;
import com.evoting.repository.VoterProfileRepository;
import com.evoting.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication service — registration, login, MFA.
 * Business logic ONLY here — no logic in controllers or repositories.
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final VoterProfileRepository voterProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            VoterProfileRepository voterProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.voterProfileRepository = voterProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new voter.
     * Creates User + VoterProfile (with opaque voter_reference) atomically.
     */
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
            request.getUsername(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            UserRole.VOTER
        );
        user = userRepository.save(user);

        // Create voter profile with opaque reference
        VoterProfile profile = new VoterProfile();
        profile.setUser(user);
        profile.setVoterReference(UUID.randomUUID().toString().replace("-", ""));
        voterProfileRepository.save(profile);

        log.info("New voter registered: username={}", user.getUsername());

        return AuthResponse.builder()
            .username(user.getUsername())
            .role(user.getRole().name())
            .message("Registration successful. Please log in.")
            .build();
    }

    /**
     * Authenticate a user and issue JWT.
     * Increments failed attempt counter on failure; locks account after MAX_FAILED_ATTEMPTS.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.isAccountLocked()) {
            throw new BadCredentialsException("Account is temporarily locked. Please try again later.");
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Reset failure counter on success
        userRepository.resetFailedLoginAttempts(user.getId(), Instant.now());

        // If MFA enabled, issue a short-lived pre-auth token
        if (user.isMfaEnabled()) {
            String preAuthToken = jwtTokenProvider.generateToken(
                user.getUsername() + "::PRE_AUTH", user.getId(), user.getRole());
            return AuthResponse.builder()
                .mfaRequired(true)
                .preAuthToken(preAuthToken)
                .message("MFA verification required")
                .build();
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), user.getRole());
        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .expiresInSeconds(900)
            .username(user.getUsername())
            .role(user.getRole().name())
            .mfaRequired(false)
            .build();
    }

    private void handleFailedLogin(User user) {
        userRepository.incrementFailedLoginAttempts(user.getId(), Instant.now());
        if (user.getFailedLoginAttempts() + 1 >= MAX_FAILED_ATTEMPTS) {
            Instant lockUntil = Instant.now().plusSeconds(LOCKOUT_MINUTES * 60L);
            userRepository.lockAccount(user.getId(), lockUntil, Instant.now());
            log.warn("Account locked for {} minutes: username={}", LOCKOUT_MINUTES, user.getUsername());
        }
    }
}
