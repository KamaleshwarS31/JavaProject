package com.evoting.controller;

import com.evoting.dto.response.BallotStatusResponse;
import com.evoting.dto.response.MerkleProofResponse;
import com.evoting.service.BallotService;
import com.evoting.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public verification portal controller.
 * All endpoints are public — no authentication required.
 * Allows independent verification of ballot inclusion in Merkle tree.
 */
@RestController
@RequestMapping("/api/v1/verify")
@Tag(name = "Verification", description = "Public ballot verification and Merkle proof endpoints")
public class VerificationController {

    private final BallotService ballotService;
    private final VerificationService verificationService;

    public VerificationController(BallotService ballotService, VerificationService verificationService) {
        this.ballotService = ballotService;
        this.verificationService = verificationService;
    }

    @GetMapping("/ballot/{ballotReference}")
    @Operation(summary = "Verify a ballot reference exists and retrieve its status (public)")
    public ResponseEntity<BallotStatusResponse> verifyBallot(@PathVariable String ballotReference) {
        return ResponseEntity.ok(ballotService.getBallotStatus(ballotReference));
    }

    @GetMapping("/merkle-proof/{ballotReference}")
    @Operation(summary = "Get Merkle inclusion proof for a ballot commitment (public)")
    public ResponseEntity<MerkleProofResponse> getMerkleProof(@PathVariable String ballotReference) {
        return ResponseEntity.ok(verificationService.getMerkleProof(ballotReference));
    }

    @GetMapping("/commitment/{commitment}")
    @Operation(summary = "Verify a commitment hash is in the system (public)")
    public ResponseEntity<BallotStatusResponse> verifyByCommitment(@PathVariable String commitment) {
        return ResponseEntity.ok(verificationService.verifyByCommitment(commitment));
    }
}
