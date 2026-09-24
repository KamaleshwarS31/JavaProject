package com.evoting.core.domain.enums;

/**
 * RBAC roles for the e-voting system.
 * Deny-by-default: any endpoint not explicitly permitted is denied.
 */
public enum UserRole {
    VOTER,
    ELECTION_ADMIN,
    ELIGIBILITY_AUTHORITY,
    BALLOT_AUTHORITY,
    TALLY_AUTHORITY,
    AUDITOR,
    SYSTEM_OPERATOR;

    /** Spring Security role prefix helper */
    public String toSpringRole() {
        return "ROLE_" + this.name();
    }
}
