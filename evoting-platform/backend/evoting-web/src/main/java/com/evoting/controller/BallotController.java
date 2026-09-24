package com.evoting.controller;

import com.evoting.dto.request.BallotSubmissionRequest;
import com.evoting.dto.response.BallotStatusResponse;
import com.evoting.dto.response.ReceiptResponse;
import com.evoting.service.BallotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Ballot ingestion REST controller.
 * Accepts encrypted ballots and returns privacy-preserving receipts.
 * No business logic — all delegated to BallotService.
 */
@RestController
@RequestMapping("/api/v1/ballots")
@Tag(name = "Ballots", description = "Encrypted ballot submission and status")
@SecurityRequirement(name = "bearerAuth")
public class BallotController {

    private final BallotService ballotService;

    public BallotController(BallotService ballotService) {
        this.ballotService = ballotService;
    }

    @PostMapping("/cast")
    @PreAuthorize("hasRole('VOTER')")
    @Operation(summary = "Cast an encrypted ballot",
               description = "Submit RSA-OAEP encrypted ballot with nullifier and commitment. Returns a privacy-preserving receipt.")
    public ResponseEntity<ReceiptResponse> castBallot(@Valid @RequestBody BallotSubmissionRequest request) {
        ReceiptResponse receipt = ballotService.submitBallot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @GetMapping("/status/{ballotReference}")
    @PreAuthorize("hasRole('VOTER')")
    @Operation(summary = "Check ballot lifecycle status")
    public ResponseEntity<BallotStatusResponse> getBallotStatus(@PathVariable String ballotReference) {
        return ResponseEntity.ok(ballotService.getBallotStatus(ballotReference));
    }
}
