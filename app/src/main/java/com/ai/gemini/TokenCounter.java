package com.ai.gemini;

import android.content.SharedPreferences;
import java.util.Calendar;

/**
 * Tracks token usage for Gemini API
 * Model limit: 123,999 TPM (tokens per minute)
 * We track daily usage to prevent quota exhaustion
 * Assuming ~2M tokens per day budget for production use
 */
public class TokenCounter {
    private static final long DAILY_TOKEN_LIMIT = 2_000_000; // Conservative daily limit
    private static final long MINUTE_TOKEN_LIMIT = 123_999;
    
    private long totalTokensUsedToday = 0;
    private long minuteWindowStart = System.currentTimeMillis();
    private long tokensUsedThisMinute = 0;

    /**
     * Check if we can use the requested tokens
     */
    public synchronized boolean canUseTokens(long tokenCount) {
        // Check daily limit
        if (totalTokensUsedToday + tokenCount > DAILY_TOKEN_LIMIT) {
            return false;
        }

        // Check minute limit
        long now = System.currentTimeMillis();
        if (now - minuteWindowStart >= 60_000) {
            minuteWindowStart = now;
            tokensUsedThisMinute = 0;
        }

        if (tokensUsedThisMinute + tokenCount > MINUTE_TOKEN_LIMIT) {
            return false;
        }

        return true;
    }

    /**
     * Record token usage
     */
    public synchronized void recordTokenUsage(long tokenCount) {
        totalTokensUsedToday += tokenCount;
        tokensUsedThisMinute += tokenCount;
    }

    /**
     * Get remaining daily tokens
     */
    public synchronized long getRemainingDailyTokens() {
        return Math.max(0, DAILY_TOKEN_LIMIT - totalTokensUsedToday);
    }

    /**
     * Get remaining minute tokens
     */
    public synchronized long getRemainingMinuteTokens() {
        long now = System.currentTimeMillis();
        if (now - minuteWindowStart >= 60_000) {
            return MINUTE_TOKEN_LIMIT;
        }
        return Math.max(0, MINUTE_TOKEN_LIMIT - tokensUsedThisMinute);
    }

    /**
     * Get percentage of daily tokens used
     */
    public synchronized double getDailyTokenUsagePercentage() {
        return (double) totalTokensUsedToday / DAILY_TOKEN_LIMIT * 100;
    }

    /**
     * Estimate tokens for a content string
     * Rule of thumb: ~1 token per 4 characters for English text
     * For code: ~1 token per 3 characters
     */
    public long estimateTokens(Object content) {
        if (content == null) {
            return 0;
        }

        String contentStr = content.toString();
        // Conservative estimate: 1 token per 3 characters
        return (long) Math.ceil(contentStr.length() / 3.0);
    }

    /**
     * Reset daily counter (called at midnight)
     */
    public synchronized void resetDailyCounter() {
        totalTokensUsedToday = 0;
    }

    /**
     * Get usage statistics
     */
    public synchronized String getUsageStats() {
        return String.format(
                "Daily: %d / %d tokens (%.1f%%) | Minute: %d / %d tokens",
                totalTokensUsedToday,
                DAILY_TOKEN_LIMIT,
                getDailyTokenUsagePercentage(),
                tokensUsedThisMinute,
                MINUTE_TOKEN_LIMIT
        );
    }
}
