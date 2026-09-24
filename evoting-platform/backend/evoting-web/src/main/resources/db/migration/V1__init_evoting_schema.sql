-- ============================================================
-- E-Voting Platform - Flyway Migration V1
-- Initial Schema for Supabase PostgreSQL 17
-- ============================================================
-- SECURITY INVARIANT: There is NO table or constraint that
-- directly maps voter_id -> candidate_id for a cast ballot.
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- Table: users
-- Purpose: Authentication credentials and role assignment.
-- Contains NO election-specific or vote-specific information.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.users (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username               VARCHAR(50)  NOT NULL,
    email                  VARCHAR(255) NOT NULL,
    password_hash          VARCHAR(255) NOT NULL,
    role                   VARCHAR(30)  NOT NULL DEFAULT 'VOTER',
    mfa_secret             VARCHAR(64),
    mfa_enabled            BOOLEAN     NOT NULL DEFAULT FALSE,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts  INTEGER     NOT NULL DEFAULT 0,
    locked_until           TIMESTAMPTZ,
    version                BIGINT      NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('VOTER','ELECTION_ADMIN','ELIGIBILITY_AUTHORITY','BALLOT_AUTHORITY','TALLY_AUTHORITY','AUDITOR','SYSTEM_OPERATOR')),
    CONSTRAINT chk_users_status  CHECK (status IN ('ACTIVE','LOCKED','SUSPENDED','DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_users_username ON public.users (username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON public.users (email);
CREATE INDEX IF NOT EXISTS idx_users_role     ON public.users (role);

-- ============================================================
-- Table: voter_profiles
-- Purpose: Restricted voter identity. STRICTLY access-controlled.
-- ONLY the Eligibility Authority service may read this table.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.voter_profiles (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    voter_reference      VARCHAR(64) NOT NULL,   -- Opaque random reference (not user_id)
    national_id_hash     VARCHAR(64),             -- SHA-256 hash of national ID ONLY
    verification_status  VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at          TIMESTAMPTZ,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_voter_profile_user_id  UNIQUE (user_id),
    CONSTRAINT uq_voter_reference        UNIQUE (voter_reference),
    CONSTRAINT chk_voter_status          CHECK (verification_status IN ('PENDING_VERIFICATION','VERIFIED','REJECTED','REVOKED'))
);

-- ============================================================
-- Table: elections
-- Purpose: Election configuration and state management.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.elections (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    election_code         VARCHAR(50)  NOT NULL,
    title                 VARCHAR(255) NOT NULL,
    description           TEXT,
    state                 VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    election_type         VARCHAR(30)  NOT NULL DEFAULT 'SINGLE_CHOICE',
    public_key_pem        TEXT,                    -- RSA-4096 public key for ballot encryption
    start_time            TIMESTAMPTZ,
    end_time              TIMESTAMPTZ,
    batch_size_limit      INTEGER     NOT NULL DEFAULT 100,
    blockchain_election_id VARCHAR(100),
    version               BIGINT      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_election_code  UNIQUE (election_code),
    CONSTRAINT chk_election_state CHECK (state IN ('DRAFT','READY','ACTIVE','CLOSED','TALLYING','FINALIZED')),
    CONSTRAINT chk_election_times CHECK (end_time IS NULL OR start_time IS NULL OR end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_election_state ON public.elections (state);
CREATE INDEX IF NOT EXISTS idx_election_code  ON public.elections (election_code);

-- ============================================================
-- Table: candidates
-- Purpose: Candidate information (public data).
-- ============================================================
CREATE TABLE IF NOT EXISTS public.candidates (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id    UUID        NOT NULL REFERENCES public.elections(id) ON DELETE CASCADE,
    candidate_code VARCHAR(20)  NOT NULL,
    display_name   VARCHAR(255) NOT NULL,
    biography      TEXT,
    party_name     VARCHAR(100),
    symbol_uri     VARCHAR(500),
    ballot_order   INTEGER     NOT NULL DEFAULT 0,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_candidate_code UNIQUE (election_id, candidate_code),
    CONSTRAINT chk_candidate_status CHECK (status IN ('ACTIVE','WITHDRAWN','DISQUALIFIED'))
);

CREATE INDEX IF NOT EXISTS idx_candidate_election ON public.candidates (election_id);

-- ============================================================
-- Table: eligibility_records
-- Purpose: Maps voter_reference (opaque) to election eligibility.
-- Does NOT link to ballot_metadata. Only EA can join to voter_profiles.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.eligibility_records (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id     UUID        NOT NULL REFERENCES public.elections(id),
    voter_reference VARCHAR(64) NOT NULL,         -- Opaque, NOT user_id
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_eligibility    UNIQUE (election_id, voter_reference),
    CONSTRAINT chk_eligibility_status CHECK (status IN ('ELIGIBLE','INELIGIBLE','PENDING_VERIFICATION','REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_eligibility_election  ON public.eligibility_records (election_id);
CREATE INDEX IF NOT EXISTS idx_eligibility_voter_ref ON public.eligibility_records (voter_reference);

-- ============================================================
-- Table: credentials
-- Purpose: Anonymous credential issuance records.
-- No link to ballot_metadata.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.credentials (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id           UUID        NOT NULL REFERENCES public.elections(id),
    voter_reference       VARCHAR(64) NOT NULL,   -- Maps to eligibility_records (EA only)
    credential_reference  VARCHAR(64) NOT NULL,
    credential_token_hash VARCHAR(64) NOT NULL,   -- SHA-256 of credential token (never store plaintext)
    status                VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    issued_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at            TIMESTAMPTZ,
    redeemed_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_credential_reference UNIQUE (credential_reference),
    CONSTRAINT uq_credential_per_voter UNIQUE (election_id, voter_reference),
    CONSTRAINT chk_credential_status   CHECK (status IN ('ISSUED','REDEEMED','EXPIRED','REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_credential_election ON public.credentials (election_id);
CREATE INDEX IF NOT EXISTS idx_credential_ref      ON public.credentials (credential_reference);

-- ============================================================
-- Table: nullifiers
-- Purpose: Double-vote prevention via election-specific nullifier.
-- N = SHA256(credential_secret || election_id)
-- SECURITY: nullifier_hash cannot be reversed to voter identity.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.nullifiers (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id      UUID        NOT NULL REFERENCES public.elections(id),
    nullifier_hash   VARCHAR(64) NOT NULL,        -- SHA-256 hex; cannot be reversed to identity
    status           VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    blockchain_tx_id VARCHAR(100),
    first_seen_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- THIS UNIQUE CONSTRAINT IS THE PRIMARY DOUBLE-VOTE PREVENTION MECHANISM
    CONSTRAINT uq_election_nullifier UNIQUE (election_id, nullifier_hash),
    CONSTRAINT chk_nullifier_status  CHECK (status IN ('ACTIVE','BLOCKCHAIN_CONFIRMED','RECONCILIATION_PENDING'))
);

CREATE INDEX IF NOT EXISTS idx_nullifier_hash     ON public.nullifiers (nullifier_hash);
CREATE INDEX IF NOT EXISTS idx_nullifier_election ON public.nullifiers (election_id);

-- ============================================================
-- Table: merkle_batches
-- Purpose: Tracks Merkle tree batches of ballot commitments.
-- Merkle root is anchored to Hyperledger Fabric.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.merkle_batches (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id         UUID        NOT NULL REFERENCES public.elections(id),
    batch_reference     VARCHAR(64) NOT NULL,
    leaf_count          INTEGER     NOT NULL DEFAULT 0,
    merkle_root         VARCHAR(64),              -- SHA-256 hex of Merkle root
    previous_batch_root VARCHAR(64),              -- Chain-links batches for audit
    blockchain_tx_id    VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    anchored_at         TIMESTAMPTZ,

    CONSTRAINT uq_batch_reference UNIQUE (batch_reference),
    CONSTRAINT chk_batch_status   CHECK (status IN ('OPEN','PENDING_ANCHOR','ANCHORED','VERIFIED','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_batch_election ON public.merkle_batches (election_id);
CREATE INDEX IF NOT EXISTS idx_batch_status   ON public.merkle_batches (status);

-- ============================================================
-- Table: ballot_metadata
-- ============================================================
-- CRITICAL SECURITY INVARIANT:
-- This table has NO voter_id, NO user_id, NO candidate_id.
-- There is NO foreign key to users, voter_profiles, or candidates.
-- This is the architectural guarantee of ballot secrecy.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.ballot_metadata (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id     UUID        NOT NULL REFERENCES public.elections(id),
    batch_id        UUID        REFERENCES public.merkle_batches(id),

    -- Opaque random UUID — included in voter receipt
    ballot_reference VARCHAR(64) NOT NULL,

    -- C = SHA256(encryptedBallot || randomSalt)
    -- Verifiable against Merkle tree without revealing choice
    commitment       VARCHAR(64) NOT NULL,

    -- RSA-OAEP encrypted ballot payload
    -- After tally finalization, may be archived per data retention policy
    encrypted_payload TEXT,

    leaf_index       INTEGER,                     -- Position in Merkle batch
    lifecycle_state  VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    idempotency_key  VARCHAR(64),                 -- X-Request-Id for replay protection
    accepted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    anchored_at      TIMESTAMPTZ,

    CONSTRAINT uq_ballot_reference  UNIQUE (ballot_reference),
    CONSTRAINT uq_ballot_commitment UNIQUE (commitment),
    CONSTRAINT chk_ballot_state     CHECK (lifecycle_state IN (
        'CREATED','PROOF_VALIDATED','BALLOT_ACCEPTED','BATCHED',
        'BLOCKCHAIN_ANCHORED','TALLY_INCLUDED','FINALIZED','REJECTED'
    ))

    -- NOTE: There is intentionally NO foreign key to users or voter_profiles.
    -- This is not an oversight. It is the core privacy design of this system.
);

CREATE INDEX IF NOT EXISTS idx_ballot_election    ON public.ballot_metadata (election_id);
CREATE INDEX IF NOT EXISTS idx_ballot_commitment  ON public.ballot_metadata (commitment);
CREATE INDEX IF NOT EXISTS idx_ballot_batch       ON public.ballot_metadata (batch_id);
CREATE INDEX IF NOT EXISTS idx_ballot_state       ON public.ballot_metadata (lifecycle_state);
CREATE INDEX IF NOT EXISTS idx_ballot_idempotency ON public.ballot_metadata (idempotency_key);

-- ============================================================
-- Table: audit_anchors
-- Purpose: Independent audit digests anchored on Fabric.
-- A = SHA256(electionState || batchRoots || metadata)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.audit_anchors (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id         UUID        NOT NULL REFERENCES public.elections(id),
    anchor_reference    VARCHAR(64) NOT NULL,
    audit_digest        VARCHAR(64) NOT NULL,
    batch_range         VARCHAR(255),
    blockchain_tx_id    VARCHAR(100),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    mismatch_detected   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_anchor_reference UNIQUE (anchor_reference),
    CONSTRAINT chk_anchor_status   CHECK (verification_status IN ('PENDING','VERIFIED','MISMATCH','INVESTIGATING'))
);

CREATE INDEX IF NOT EXISTS idx_anchor_election ON public.audit_anchors (election_id);

-- ============================================================
-- Table: system_events (Append-only audit log)
-- NEVER logs: passwords, tokens, keys, ballot content
-- ============================================================
CREATE TABLE IF NOT EXISTS public.system_events (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type     VARCHAR(50) NOT NULL,
    actor_reference VARCHAR(64),           -- Anonymized actor (not user_id)
    election_id    UUID,
    event_hash     VARCHAR(64),            -- SHA-256 of event for tamper evidence
    metadata_json  TEXT,
    correlation_id VARCHAR(64),
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_type      ON public.system_events (event_type);
CREATE INDEX IF NOT EXISTS idx_event_election  ON public.system_events (election_id);
CREATE INDEX IF NOT EXISTS idx_event_timestamp ON public.system_events (event_timestamp);

-- ============================================================
-- Table: security_events
-- ============================================================
CREATE TABLE IF NOT EXISTS public.security_events (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type     VARCHAR(50) NOT NULL,
    severity       VARCHAR(20) NOT NULL DEFAULT 'LOW',
    client_ip      VARCHAR(45),
    correlation_id VARCHAR(64),
    metadata_json  TEXT,
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_security_event_type      ON public.security_events (event_type);
CREATE INDEX IF NOT EXISTS idx_security_event_severity  ON public.security_events (severity);
CREATE INDEX IF NOT EXISTS idx_security_event_timestamp ON public.security_events (event_timestamp);

-- ============================================================
-- Verification: Assert the security invariant holds.
-- This view can be queried to confirm no ballot-voter linkage exists.
-- ============================================================
CREATE OR REPLACE VIEW public.v_security_invariant_check AS
SELECT
    'ballot_metadata_has_no_voter_id'   AS invariant,
    NOT EXISTS (
        SELECT column_name
        FROM information_schema.columns
        WHERE table_name = 'ballot_metadata'
          AND column_name IN ('voter_id', 'user_id', 'candidate_id')
    ) AS satisfied;

-- ============================================================
-- Comments for documentation
-- ============================================================
COMMENT ON TABLE public.ballot_metadata IS
    'Privacy-preserving ballot record. Contains NO voter_id, user_id, or candidate_id by design.';
COMMENT ON TABLE public.nullifiers IS
    'Double-vote prevention. nullifier_hash = SHA256(credential_secret || election_id). Not reversible to voter identity.';
COMMENT ON TABLE public.voter_profiles IS
    'RESTRICTED: Voter identity. Only Eligibility Authority service may access. Never joined to ballot_metadata.';
