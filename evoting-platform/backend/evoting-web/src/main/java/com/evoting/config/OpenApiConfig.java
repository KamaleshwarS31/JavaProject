package com.evoting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 3.1 configuration.
 * Accessible at: /swagger-ui/index.html and /api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI evotingOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("E-Voting Platform API")
                .version("1.0.0")
                .description("""
                    Privacy-Preserving End-to-End Verifiable Blockchain-Based E-Voting System.
                    
                    ## Security Model
                    - JWT Bearer token authentication (15-minute TTL)
                    - Ballot privacy: No voter identity linked to any ballot
                    - Double-vote prevention: Nullifier = SHA256(credential_secret || election_id)
                    - Public verifiability: Merkle proofs anchored on Hyperledger Fabric
                    
                    ## Privacy Guarantee
                    The `BallotMetadata` table has NO `voter_id`, `user_id`, or `candidate_id`.
                    This is enforced at the database schema level and independently verifiable.
                    """)
            )
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token from /api/v1/auth/login")
                )
            );
    }
}
