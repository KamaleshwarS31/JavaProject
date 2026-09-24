# Security Policy — E-Voting Platform

## Privacy Architecture

The core security invariant of this system is:

> **There is no database record that directly maps a voter's identity to their ballot choice.**

### Enforcement Layers

| Layer | Mechanism |
|---|---|
| Database schema | `ballot_metadata` has no `voter_id`, `user_id`, or `candidate_id` columns |
| Application code | `BallotService` never receives voter identity — only a credential token |
| Cryptographic | Nullifier = SHA-256(credential_secret \| electionId) — not reversible |
| Blockchain | Fabric only stores nullifier hashes and Merkle roots — no identity |

### Verifying the Invariant

Anyone can verify the schema-level guarantee:
```sql
SELECT * FROM public.verify_security_invariant();
-- Returns: invariant=ballot_metadata_has_no_voter_id | satisfied=true
```

## Cryptographic Algorithms

| Purpose | Algorithm | Key Size |
|---|---|---|
| Ballot encryption | RSA-OAEP / SHA-256 (BouncyCastle) | 4096-bit |
| Password hashing | Argon2id | Memory=16MB, Iterations=2, Parallelism=1 |
| Nullifier | SHA-256 (domain-separated) | 256-bit output |
| Commitment | SHA-256 (domain-separated) | 256-bit output |
| Merkle tree | SHA-256 (leaf and node prefixes) | 256-bit output |
| JWT signing | HMAC-SHA512 | 256-bit+ secret |

## Reporting Vulnerabilities

Do **not** open public GitHub issues for security vulnerabilities.
Email: skamaleshwar31@gmail.com with subject line `[SECURITY] E-Voting Platform`.

## Threat Model Summary

- **Coercion resistance**: Receipts cannot prove how you voted
- **Privacy**: No voter-ballot linkage exists anywhere in the system
- **Double-vote prevention**: Nullifiers checked at both DB and Fabric layers
- **Integrity**: Merkle roots anchored on-chain; independently verifiable
- **Availability**: Rate limiting prevents DoS on ballot submission
