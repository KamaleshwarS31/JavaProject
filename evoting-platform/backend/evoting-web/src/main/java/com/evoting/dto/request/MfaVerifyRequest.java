package com.evoting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MfaVerifyRequest {

    @NotBlank
    private String preAuthToken; // JWT issued after password verification, before MFA

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "MFA code must be exactly 6 digits")
    private String totpCode;
}
