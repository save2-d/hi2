package com.ai.gemini;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages Gemini API Keys with encryption and failover support
 * - Stores up to 2 API keys (primary + backup)
 * - Encrypts keys at rest using EncryptedSharedPreferences
 * - Tracks key usage and failures
 * - Handles key rotation
 */
public class APIKeyManager {
    private static final String TAG = "APIKeyManager";
    private static final String PREFS_NAME = "gemini_api_keys";
    private static final String KEY_PRIMARY = "api_key_primary";
    private static final String KEY_BACKUP = "api_key_backup";
    private static final String KEY_CURRENT = "api_key_current";
    private static final String KEY_PRIMARY_FAILURES = "primary_failures";
    private static final String KEY_BACKUP_FAILURES = "backup_failures";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";
    private static final String KEY_SETUP_TIMESTAMP = "setup_timestamp";

    private final Context context;
    private final EncryptedSharedPreferences prefs;

    public APIKeyManager(Context context) {
        this.context = context;
        this.prefs = getEncryptedPreferences();
    }

    /**
     * Initialize encrypted shared preferences
     */
    private EncryptedSharedPreferences getEncryptedPreferences() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize encrypted preferences", e);
        }
    }

    /**
     * Set primary API key (called on first setup or key rotation)
     */
    public void setPrimaryApiKey(String apiKey) {
        if (apiKey != null && !apiKey.isEmpty()) {
            prefs.edit()
                    .putString(KEY_PRIMARY, apiKey)
                    .putString(KEY_CURRENT, KEY_PRIMARY)
                    .putInt(KEY_PRIMARY_FAILURES, 0)
                    .apply();
        }
    }

    /**
     * Set backup API key
     */
    public void setBackupApiKey(String apiKey) {
        if (apiKey != null && !apiKey.isEmpty()) {
            prefs.edit()
                    .putString(KEY_BACKUP, apiKey)
                    .putInt(KEY_BACKUP_FAILURES, 0)
                    .apply();
        }
    }

    /**
     * Get current primary API key
     */
    public String getPrimaryApiKey() {
        return prefs.getString(KEY_PRIMARY, "");
    }

    /**
     * Get backup API key
     */
    public String getBackupApiKey() {
        return prefs.getString(KEY_BACKUP, "");
    }

    /**
     * Get currently active API key
     */
    public String getCurrentApiKey() {
        String current = prefs.getString(KEY_CURRENT, KEY_PRIMARY);
        if (current.equals(KEY_PRIMARY)) {
            return getPrimaryApiKey();
        } else {
            return getBackupApiKey();
        }
    }

    /**
     * Check if API keys are configured
     */
    public boolean hasApiKeys() {
        return !getPrimaryApiKey().isEmpty();
    }

    /**
     * Check if backup key is configured
     */
    public boolean hasBackupKey() {
        return !getBackupApiKey().isEmpty();
    }

    /**
     * Record successful API call with a key
     */
    public void recordKeyUsage(String apiKey, boolean success) {
        if (!success) {
            if (apiKey.equals(getPrimaryApiKey())) {
                int failures = prefs.getInt(KEY_PRIMARY_FAILURES, 0);
                prefs.edit().putInt(KEY_PRIMARY_FAILURES, failures + 1).apply();
            } else if (apiKey.equals(getBackupApiKey())) {
                int failures = prefs.getInt(KEY_BACKUP_FAILURES, 0);
                prefs.edit().putInt(KEY_BACKUP_FAILURES, failures + 1).apply();
            }
        } else {
            // Reset failure counter on success
            if (apiKey.equals(getPrimaryApiKey())) {
                prefs.edit().putInt(KEY_PRIMARY_FAILURES, 0).apply();
            } else if (apiKey.equals(getBackupApiKey())) {
                prefs.edit().putInt(KEY_BACKUP_FAILURES, 0).apply();
            }
        }
    }

    /**
     * Switch to backup key if primary fails
     */
    public void switchToBackupKey() {
        if (hasBackupKey()) {
            prefs.edit().putString(KEY_CURRENT, KEY_BACKUP).apply();
        }
    }

    /**
     * Switch back to primary key
     */
    public void switchToPrimaryKey() {
        prefs.edit().putString(KEY_CURRENT, KEY_PRIMARY).apply();
    }

    /**
     * Get failure count for a key
     */
    public int getFailureCount(String keyType) {
        if (keyType.equals(KEY_PRIMARY)) {
            return prefs.getInt(KEY_PRIMARY_FAILURES, 0);
        } else {
            return prefs.getInt(KEY_BACKUP_FAILURES, 0);
        }
    }

    /**
     * Clear all API keys (for uninstall or reset)
     */
    public void clearAllKeys() {
        prefs.edit()
                .remove(KEY_PRIMARY)
                .remove(KEY_BACKUP)
                .remove(KEY_CURRENT)
                .remove(KEY_PRIMARY_FAILURES)
                .remove(KEY_BACKUP_FAILURES)
                .apply();
    }

    /**
     * Get setup status
     */
    public boolean isSetupComplete() {
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false);
    }

    /**
     * Mark setup as complete
     */
    public void markSetupComplete() {
        prefs.edit()
                .putBoolean(KEY_SETUP_COMPLETE, true)
                .putLong(KEY_SETUP_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    /**
     * Get detailed key status for UI display
     */
    public Map<String, Object> getKeyStatus() {
        Map<String, Object> status = new HashMap<>();
        
        String primaryKey = getPrimaryApiKey();
        String backupKey = getBackupApiKey();
        
        status.put("primary_configured", !primaryKey.isEmpty());
        status.put("primary_masked", maskApiKey(primaryKey));
        status.put("primary_failures", getFailureCount(KEY_PRIMARY));
        
        status.put("backup_configured", !backupKey.isEmpty());
        status.put("backup_masked", maskApiKey(backupKey));
        status.put("backup_failures", getFailureCount(KEY_BACKUP));
        
        status.put("current_key", prefs.getString(KEY_CURRENT, KEY_PRIMARY));
        status.put("setup_complete", isSetupComplete());
        
        return status;
    }

    /**
     * Mask API key for display (show only first and last 4 chars)
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
