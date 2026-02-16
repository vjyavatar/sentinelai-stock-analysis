package com.sentinel.stockanalysis.service;

import com.sentinel.stockanalysis.entity.User;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limit Service
 * 
 * Implements token bucket algorithm for fair rate limiting
 * Different limits for free vs premium users
 */
@Service
@Slf4j
public class RateLimitService {

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    // Rate limits
    private static final int FREE_USER_REQUESTS_PER_HOUR = 10;
    private static final int PREMIUM_USER_REQUESTS_PER_HOUR = 100;

    /**
     * Check if user can make a request
     */
    public boolean allowRequest(User user) {
        Bucket bucket = resolveBucket(user);
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("⚠️ Rate limit exceeded for user: {}", user.getEmail());
        }
        
        return allowed;
    }

    /**
     * Get or create bucket for user
     */
    private Bucket resolveBucket(User user) {
        return buckets.computeIfAbsent(user.getId(), id -> createBucket(user));
    }

    /**
     * Create rate limit bucket based on user tier
     */
    private Bucket createBucket(User user) {
        int requestsPerHour = user.isPremium() ? 
                PREMIUM_USER_REQUESTS_PER_HOUR : 
                FREE_USER_REQUESTS_PER_HOUR;

        Bandwidth limit = Bandwidth.classic(
                requestsPerHour,
                Refill.intervally(requestsPerHour, Duration.ofHours(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Get remaining requests for user
     */
    public long getRemainingRequests(User user) {
        Bucket bucket = resolveBucket(user);
        return bucket.getAvailableTokens();
    }

    /**
     * Reset rate limit for user (admin function)
     */
    public void resetLimit(Long userId) {
        buckets.remove(userId);
        log.info("✅ Rate limit reset for user ID: {}", userId);
    }
}
