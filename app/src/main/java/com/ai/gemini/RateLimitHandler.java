package com.ai.gemini;

/**
 * Handles rate limiting for Gemini API
 * Model limits:
 * - Gemini 2.5 Pro: 1.8 RPM (requests per minute), 123,999 TPM (tokens per minute)
 * - This handler ensures we don't exceed RPM limits
 */
public class RateLimitHandler {
    private static final int MAX_REQUESTS_PER_MINUTE = 1; // Conservative: allow 1 request per minute
    private static final long MINUTE_MS = 60_000;
    
    private long lastRequestTime = 0;
    private int requestCount = 0;
    private long windowStartTime = System.currentTimeMillis();

    /**
     * Check if we can make a request without exceeding rate limit
     */
    public synchronized boolean canMakeRequest() {
        long now = System.currentTimeMillis();
        long timeElapsed = now - windowStartTime;

        // Reset window if a minute has passed
        if (timeElapsed >= MINUTE_MS) {
            windowStartTime = now;
            requestCount = 0;
            return true;
        }

        // Check if we've exceeded max requests
        if (requestCount >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }

        return true;
    }

    /**
     * Record a successful request
     */
    public synchronized void recordSuccessfulRequest() {
        long now = System.currentTimeMillis();
        long timeElapsed = now - windowStartTime;

        // Reset window if needed
        if (timeElapsed >= MINUTE_MS) {
            windowStartTime = now;
            requestCount = 0;
        }

        requestCount++;
        lastRequestTime = now;
    }

    /**
     * Get how long to wait before next request (in milliseconds)
     */
    public synchronized long getWaitTimeMs() {
        long now = System.currentTimeMillis();
        long timeElapsed = now - windowStartTime;
        
        if (timeElapsed >= MINUTE_MS) {
            return 0; // Can request immediately
        }

        return MINUTE_MS - timeElapsed;
    }

    /**
     * Get remaining requests in current window
     */
    public synchronized int getRemainingRequests() {
        long now = System.currentTimeMillis();
        long timeElapsed = now - windowStartTime;

        if (timeElapsed >= MINUTE_MS) {
            return MAX_REQUESTS_PER_MINUTE;
        }

        return Math.max(0, MAX_REQUESTS_PER_MINUTE - requestCount);
    }

    /**
     * Get time until rate limit window resets (in seconds)
     */
    public synchronized long getWindowResetSeconds() {
        long now = System.currentTimeMillis();
        long timeElapsed = now - windowStartTime;
        
        if (timeElapsed >= MINUTE_MS) {
            return 0;
        }

        return (MINUTE_MS - timeElapsed) / 1000;
    }

    /**
     * Reset rate limit counters (useful for testing)
     */
    public synchronized void reset() {
        lastRequestTime = 0;
        requestCount = 0;
        windowStartTime = System.currentTimeMillis();
    }
}
