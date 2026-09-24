-- ============================================================
-- Flyway V2: Performance indexes and additional constraints
-- ============================================================

-- Composite index for common eligibility check query
CREATE INDEX IF NOT EXISTS idx_eligibility_election_voter_status
    ON public.eligibility_records (election_id, voter_reference, status);

-- Index for credential lookup by voter_reference in an election
CREATE INDEX IF NOT EXISTS idx_credential_election_voter
    ON public.credentials (election_id, voter_reference);

-- Index for ballot count by state per election
CREATE INDEX IF NOT EXISTS idx_ballot_election_state
    ON public.ballot_metadata (election_id, lifecycle_state);

-- Partial index: only non-anchored batches (used by scheduler)
CREATE INDEX IF NOT EXISTS idx_batch_open
    ON public.merkle_batches (election_id, status)
    WHERE status IN ('OPEN', 'PENDING_ANCHOR');

-- Partial index: only active credentials
CREATE INDEX IF NOT EXISTS idx_credential_active
    ON public.credentials (credential_reference)
    WHERE status = 'ISSUED';

-- Partial index: only active nullifiers
CREATE INDEX IF NOT EXISTS idx_nullifier_active
    ON public.nullifiers (election_id, nullifier_hash)
    WHERE status = 'ACTIVE';

-- Text search index on election title
CREATE INDEX IF NOT EXISTS idx_election_title_gin
    ON public.elections USING gin(to_tsvector('english', title));

-- ============================================================
-- Row Level Security (RLS) policies
-- IMPORTANT: Enable RLS on voter_profiles to restrict access
-- to only the eligibility_authority database user
-- ============================================================
ALTER TABLE public.voter_profiles ENABLE ROW LEVEL SECURITY;

-- Only the eligibility_authority role may SELECT voter_profiles
-- In Supabase, create a role named 'eligibility_authority'
-- and this policy will enforce access control at DB level
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_roles WHERE rolname = 'eligibility_authority'
    ) THEN
        -- Role doesn't exist in this environment; skip policy
        RAISE NOTICE 'Role eligibility_authority not found. RLS policy skipped.';
    ELSE
        CREATE POLICY voter_profile_rls ON public.voter_profiles
            FOR ALL
            TO eligibility_authority
            USING (true);
    END IF;
END$$;

-- ============================================================
-- Audit view: election summary (safe for public audit portal)
-- ============================================================
CREATE OR REPLACE VIEW public.v_election_audit_summary AS
SELECT
    e.id                                            AS election_id,
    e.election_code,
    e.title,
    e.state,
    e.start_time,
    e.end_time,
    COUNT(DISTINCT n.id)                            AS total_nullifiers,
    COUNT(DISTINCT bm.id)                           AS total_ballots_accepted,
    COUNT(DISTINCT mb.id)                           AS total_batches,
    SUM(CASE WHEN mb.status = 'ANCHORED' THEN 1 ELSE 0 END) AS anchored_batches,
    MIN(bm.accepted_at)                             AS first_ballot_at,
    MAX(bm.accepted_at)                             AS last_ballot_at
FROM public.elections e
LEFT JOIN public.nullifiers n         ON n.election_id = e.id
LEFT JOIN public.ballot_metadata bm   ON bm.election_id = e.id
LEFT JOIN public.merkle_batches mb    ON mb.election_id = e.id
GROUP BY e.id, e.election_code, e.title, e.state, e.start_time, e.end_time;

COMMENT ON VIEW public.v_election_audit_summary IS
    'Safe public audit view. Does not expose voter identity or ballot choices.';

-- ============================================================
-- Stored procedure: verify security invariant (for testing)
-- Returns true if ballot_metadata has no identity columns
-- ============================================================
CREATE OR REPLACE FUNCTION public.verify_security_invariant()
RETURNS TABLE(invariant TEXT, satisfied BOOLEAN) AS $$
BEGIN
    RETURN QUERY
    SELECT 'ballot_metadata_has_no_voter_id'::TEXT,
           NOT EXISTS (
               SELECT column_name
               FROM information_schema.columns
               WHERE table_schema = 'public'
                 AND table_name = 'ballot_metadata'
                 AND column_name IN ('voter_id', 'user_id', 'candidate_id', 'voter_name', 'candidate_name')
           );

    RETURN QUERY
    SELECT 'nullifier_not_linked_to_ballot'::TEXT,
           NOT EXISTS (
               SELECT 1
               FROM information_schema.table_constraints tc
               JOIN information_schema.key_column_usage kcu
                   ON tc.constraint_name = kcu.constraint_name
               WHERE tc.constraint_type = 'FOREIGN KEY'
                 AND kcu.table_name = 'nullifiers'
                 AND kcu.column_name IN (
                     SELECT column_name
                     FROM information_schema.columns
                     WHERE table_name = 'ballot_metadata'
                 )
           );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION public.verify_security_invariant() IS
    'Call this to verify the privacy design invariants hold in the database schema.';
