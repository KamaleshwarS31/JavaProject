package com.evoting.security.ratelimit;

import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiting filter using Bucket4j token bucket algorithm.
 * Applied to sensitive endpoints: auth, credential issuance, ballot submission.
 *
 * NOTE: For production deployment, use a distributed cache (Redis) instead of
 * in-memory ConcurrentHashMap to support multi-instance deployments.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // In-memory store: IP -> Bucket. Replace with Redis for multi-instance production.
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> ballotBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register")) {
            Bucket bucket = loginBuckets.computeIfAbsent(clientIp, k -> createLoginBucket());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for login from IP: {}", clientIp);
                sendRateLimitResponse(response);
                return;
            }
        } else if (path.startsWith("/api/v1/ballots/cast")) {
            // Ballot: 1 request per 30 seconds per IP (soft limit, primary protection is nullifier)
            Bucket bucket = ballotBuckets.computeIfAbsent(clientIp, k -> createBallotBucket());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for ballot submission from IP: {}", clientIp);
                sendRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createLoginBucket() {
        // 5 requests per 15 minutes per IP
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(15))
                .build())
            .build();
    }

    private Bucket createBallotBucket() {
        // 3 requests per 30 seconds per IP (generous enough for retries, tight for abuse)
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(3)
                .refillIntervally(3, Duration.ofSeconds(30))
                .build())
            .build();
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"status\":429,\"error\":\"Too Many Requests\","
            + "\"message\":\"Rate limit exceeded. Please wait before retrying.\"}"
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
