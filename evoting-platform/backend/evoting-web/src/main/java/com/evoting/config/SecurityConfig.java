package com.evoting.config;

import com.evoting.security.jwt.JwtAuthenticationFilter;
import com.evoting.security.ratelimit.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 6.x Configuration.
 *
 * Security principles:
 * - Deny-by-default authorization
 * - Stateless JWT authentication
 * - Rate limiting via Bucket4j filter
 * - Strict CORS (no wildcards)
 * - Security headers (CSP, HSTS, X-Frame-Options, etc.)
 * - Argon2id password hashing
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomUserDetailsService userDetailsService;

    @Value("${evoting.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter,
            CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Stateless JWT — no session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // CORS — strict, explicit origins only
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF — disabled for stateless JWT API
            // NOTE: If JSP form-based endpoints are added, CSRF must be enabled for those paths
            .csrf(AbstractHttpConfigurer::disable)
            // Security headers
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' data: https://cdn.jsdelivr.net; img-src 'self' data:; connect-src 'self'; object-src 'none'; frame-ancestors 'none'")
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            )
            // Authorization rules — deny-by-default
            .authorizeHttpRequests(auth -> auth
                // Allow internal JSP forward and error dispatches (Spring Security 6 requirement)
                .dispatcherTypeMatchers(
                    jakarta.servlet.DispatcherType.FORWARD,
                    jakarta.servlet.DispatcherType.ERROR,
                    jakarta.servlet.DispatcherType.INCLUDE
                ).permitAll()
                // Web UI views & static assets
                .requestMatchers("/", "/elections", "/elections/**", "/auth/**", "/ballot/**", "/audit-portal", "/audit-portal/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico", "/error", "/WEB-INF/**").permitAll()
                // Public REST endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/mfa/verify").permitAll()
                // Public read (election info, candidates, audit, verification)
                .requestMatchers(HttpMethod.GET, "/api/v1/elections").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/elections/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/verify/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/audit/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/receipts/**").permitAll()
                // Swagger / OpenAPI
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                // Actuator — health is public
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("SYSTEM_OPERATOR")
                // Admin views & operations
                .requestMatchers("/admin/**").hasAnyRole("ELECTION_ADMIN", "SYSTEM_OPERATOR")
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ELECTION_ADMIN", "SYSTEM_OPERATOR")
                // Voter operations (authenticated)
                .requestMatchers("/api/v1/credentials/**").hasAnyRole("VOTER", "ELIGIBILITY_AUTHORITY")
                .requestMatchers(HttpMethod.POST, "/api/v1/ballots/**").hasRole("VOTER")
                .requestMatchers(HttpMethod.GET, "/api/v1/ballots/**").hasRole("VOTER")
                // Everything else — deny
                .anyRequest().authenticated()
            )
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id — OWASP recommended for password hashing
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Strict CORS — explicit origins, never "*" for authenticated endpoints
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
