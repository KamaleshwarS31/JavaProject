package com.evoting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Authentication response DTO.
 * NOTE: Token is included. Sensitive data (password hash, MFA secret) is NEVER included.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresInSeconds;
    private String username;
    private String role;
    private boolean mfaRequired;
    private String preAuthToken; // Only present when MFA is required
    private String message;
}
