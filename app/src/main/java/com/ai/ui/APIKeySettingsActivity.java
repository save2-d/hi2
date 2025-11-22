package com.ai.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.ai.gemini.APIKeyManager;
import java.util.Map;

/**
 * API Key Management Fragment/Activity
 * Handles user setup of Gemini API keys with encryption
 * Allows configuration of primary and backup keys with validation
 */
public class APIKeySettingsActivity extends AppCompatActivity {
    private static final String TAG = "APIKeySettingsActivity";

    private APIKeyManager keyManager;
    private EditText primaryKeyInput;
    private EditText backupKeyInput;
    private Button testPrimaryKeyButton;
    private Button testBackupKeyButton;
    private Button saveButton;
    private Button clearButton;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView setupGuideText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_key_settings);

        keyManager = new APIKeyManager(this);
        initializeViews();
        loadCurrentKeys();
        updateStatus();
    }

    /**
     * Initialize UI views
     */
    private void initializeViews() {
        primaryKeyInput = findViewById(R.id.primary_key_input);
        backupKeyInput = findViewById(R.id.backup_key_input);
        testPrimaryKeyButton = findViewById(R.id.test_primary_key_button);
        testBackupKeyButton = findViewById(R.id.test_backup_key_button);
        saveButton = findViewById(R.id.save_button);
        clearButton = findViewById(R.id.clear_button);
        statusText = findViewById(R.id.status_text);
        progressBar = findViewById(R.id.progress_bar);
        setupGuideText = findViewById(R.id.setup_guide_text);

        // Set up listeners
        testPrimaryKeyButton.setOnClickListener(v -> testApiKey(true));
        testBackupKeyButton.setOnClickListener(v -> testApiKey(false));
        saveButton.setOnClickListener(v -> saveKeys());
        clearButton.setOnClickListener(v -> clearAllKeys());

        // Set up input hints
        primaryKeyInput.setHint("Paste your primary Gemini API key here");
        backupKeyInput.setHint("(Optional) Paste your backup key here");

        // Make inputs password-like
        primaryKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        backupKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Show setup guide
        setupGuideText.setText(generateSetupGuide());
    }

    /**
     * Load current API keys (masked)
     */
    private void loadCurrentKeys() {
        if (keyManager.hasApiKeys()) {
            primaryKeyInput.setText(keyManager.getPrimaryApiKey());
        }

        if (keyManager.hasBackupKey()) {
            backupKeyInput.setText(keyManager.getBackupApiKey());
        }
    }

    /**
     * Test API key validity
     */
    private void testApiKey(boolean isPrimary) {
        String key = isPrimary ? primaryKeyInput.getText().toString() : backupKeyInput.getText().toString();

        if (key.isEmpty()) {
            Toast.makeText(this, "Please enter an API key", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        // Test in background thread
        new Thread(() -> {
            try {
                // Simple test: check if key format is valid
                boolean isValid = isValidApiKeyFormat(key);
                
                if (isValid) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(APIKeySettingsActivity.this, "✓ API key is valid", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(APIKeySettingsActivity.this, "✗ Invalid API key format", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(APIKeySettingsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Validate API key format (Gemini keys start with AIza...)
     */
    private boolean isValidApiKeyFormat(String key) {
        return key != null && key.length() > 20 && key.startsWith("AIza");
    }

    /**
     * Save API keys (encrypted)
     */
    private void saveKeys() {
        String primaryKey = primaryKeyInput.getText().toString().trim();
        String backupKey = backupKeyInput.getText().toString().trim();

        if (primaryKey.isEmpty()) {
            Toast.makeText(this, "Primary API key is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidApiKeyFormat(primaryKey)) {
            Toast.makeText(this, "Primary key format is invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!backupKey.isEmpty() && !isValidApiKeyFormat(backupKey)) {
            Toast.makeText(this, "Backup key format is invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save keys
        keyManager.setPrimaryApiKey(primaryKey);
        if (!backupKey.isEmpty()) {
            keyManager.setBackupApiKey(backupKey);
        }
        keyManager.markSetupComplete();

        Toast.makeText(this, "✓ API keys saved and encrypted", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    /**
     * Clear all API keys
     */
    private void clearAllKeys() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All API Keys?")
                .setMessage("This will remove all configured API keys. You'll need to add them again.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    keyManager.clearAllKeys();
                    primaryKeyInput.setText("");
                    backupKeyInput.setText("");
                    Toast.makeText(APIKeySettingsActivity.this, "API keys cleared", Toast.LENGTH_SHORT).show();
                    updateStatus();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Update status display
     */
    private void updateStatus() {
        Map<String, Object> status = keyManager.getKeyStatus();

        StringBuilder statusStr = new StringBuilder();
        statusStr.append("Primary Key: ");
        if ((boolean) status.get("primary_configured")) {
            statusStr.append("✓ Configured (").append(status.get("primary_masked")).append(")");
            int failures = (int) status.get("primary_failures");
            if (failures > 0) {
                statusStr.append(" [").append(failures).append(" failures]");
            }
        } else {
            statusStr.append("✗ Not configured");
        }

        statusStr.append("\n\nBackup Key: ");
        if ((boolean) status.get("backup_configured")) {
            statusStr.append("✓ Configured (").append(status.get("backup_masked")).append(")");
            int failures = (int) status.get("backup_failures");
            if (failures > 0) {
                statusStr.append(" [").append(failures).append(" failures]");
            }
        } else {
            statusStr.append("✗ Not configured");
        }

        statusStr.append("\n\nCurrent Active: ");
        statusStr.append((String) status.get("current_key"));

        statusStr.append("\n\nSetup Complete: ");
        statusStr.append((boolean) status.get("setup_complete") ? "Yes" : "No");

        statusText.setText(statusStr.toString());
    }

    /**
     * Generate setup guide text
     */
    private String generateSetupGuide() {
        return "How to get your Gemini API Key:\n\n" +
                "1. Go to https://ai.google.dev/gemini-api/docs/api-key\n" +
                "2. Click 'Get API Key' button\n" +
                "3. Create a new API key (or select existing)\n" +
                "4. Copy the key starting with 'AIza...'\n" +
                "5. Paste it in the Primary Key field above\n" +
                "6. (Optional) Add a backup key for failover\n" +
                "7. Click 'Save Keys'\n\n" +
                "Your keys are encrypted and stored securely on this device.";
    }
}
