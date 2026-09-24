package com.evoting.controller;

import com.evoting.core.domain.enums.EligibilityStatus;
import com.evoting.service.EligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Eligibility and credential REST controller.
 * Voters check eligibility and request credentials here.
 */
@RestController
@RequestMapping("/api/v1/credentials")
@Tag(name = "Credentials", description = "Voter eligibility check and anonymous credential issuance")
@SecurityRequirement(name = "bearerAuth")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @GetMapping("/eligibility/{electionId}")
    @Operation(summary = "Check your eligibility status for an election")
    public ResponseEntity<Map<String, Object>> checkEligibility(
            @PathVariable UUID electionId,
            Authentication auth) {
        // Extract userId from JWT principal
        UUID userId = extractUserId(auth);
        EligibilityStatus status = eligibilityService.getEligibilityStatus(userId, electionId);
        return ResponseEntity.ok(Map.of(
            "electionId", electionId.toString(),
            "eligibilityStatus", status.name(),
            "eligible", status == EligibilityStatus.ELIGIBLE
        ));
    }

    @PostMapping("/issue/{electionId}")
    @Operation(summary = "Issue an anonymous credential for a specific election",
               description = "Returns a one-time credential token. Save it securely — it cannot be retrieved again.")
    public ResponseEntity<Map<String, Object>> issueCredential(
            @PathVariable UUID electionId,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        String credentialToken = eligibilityService.issueCredential(userId, electionId);
        return ResponseEntity.ok(Map.of(
            "credentialToken", credentialToken,
            "electionId", electionId.toString(),
            "warning", "Save this credential token securely. It is shown only once and cannot be recovered."
        ));
    }

    private UUID extractUserId(Authentication auth) {
        // JWT principal carries user ID — set by JwtAuthenticationFilter
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            // Fallback: use username to look up user ID
            // In a full implementation, userId is embedded in JWT claims
            return UUID.nameUUIDFromBytes(ud.getUsername().getBytes());
        }
        return (UUID) principal;
    }
}
