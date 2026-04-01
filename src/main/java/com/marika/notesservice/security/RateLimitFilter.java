package com.marika.notesservice.security;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final long capacity;
    private final long durationSeconds;

    public RateLimitFilter(
            @Value("${rate-limit.login.capacity}") long capacity,
            @Value("${rate-limit.login.duration-seconds}") long durationSeconds
    ) {
        this.capacity = capacity;
        this.durationSeconds = durationSeconds;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().equals("/login")
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            long waitForRefillSeconds = bucket.getAvailableTokens() == 0
                    ? durationSeconds
                    : 1;

            response.setStatus(TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(waitForRefillSeconds));
            response.getWriter().write("Too many login attempts. Try again later.");
        }
    }

    private Bucket newBucket(String key) {
        Refill refill = Refill.greedy(capacity, Duration.ofSeconds(durationSeconds));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
