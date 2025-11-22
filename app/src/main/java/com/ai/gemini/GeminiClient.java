package com.ai.gemini;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Gemini API Client - Handles communication with Google Gemini 2.5 Pro API
 * Supports failover, rate limiting, and token management
 */
public class GeminiClient {
    private static final String TAG = "GeminiClient";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL = "gemini-2.5-pro";
    private static final String ENDPOINT = ":generateContent";
    private static final int READ_TIMEOUT_SECONDS = 120;
    private static final int CONNECT_TIMEOUT_SECONDS = 30;

    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final APIKeyManager keyManager;
    private final RateLimitHandler rateLimitHandler;
    private final TokenCounter tokenCounter;

    public GeminiClient(Context context) {
        this.context = context;
        this.keyManager = new APIKeyManager(context);
        this.rateLimitHandler = new RateLimitHandler();
        this.tokenCounter = new TokenCounter();
        
        this.httpClient = new OkHttpClient.Builder()
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        
        this.gson = new Gson();
    }

    /**
     * Send a request to Gemini API with automatic failover
     */
    public GeminiResponse generateContent(GeminiRequest request, boolean useThinking) {
        // Check rate limit before attempting
        if (!rateLimitHandler.canMakeRequest()) {
            long waitTime = rateLimitHandler.getWaitTimeMs();
            return new GeminiResponse(
                    null,
                    new GeminiError("RATE_LIMIT", "Please wait " + (waitTime / 1000) + " seconds before next request")
            );
        }

        String primaryKey = keyManager.getPrimaryApiKey();
        String backupKey = keyManager.getBackupApiKey();

        // Attempt with primary key
        GeminiResponse response = attemptRequest(request, primaryKey, useThinking, 0);
        
        if (response.isSuccess()) {
            rateLimitHandler.recordSuccessfulRequest();
            keyManager.recordKeyUsage(primaryKey, true);
            return response;
        }

        // If primary key fails due to auth, try backup
        if (response.getError() != null && 
            (response.getError().getCode().equals("INVALID_API_KEY") || 
             response.getError().getCode().equals("PERMISSION_DENIED"))) {
            
            if (backupKey != null && !backupKey.isEmpty()) {
                response = attemptRequest(request, backupKey, useThinking, 0);
                if (response.isSuccess()) {
                    keyManager.recordKeyUsage(backupKey, true);
                    keyManager.switchToBackupKey(); // Mark backup as primary for next request
                    return response;
                }
                keyManager.recordKeyUsage(backupKey, false);
            }
        }

        keyManager.recordKeyUsage(primaryKey, false);
        return response;
    }

    /**
     * Attempt a request with retry logic
     */
    private GeminiResponse attemptRequest(GeminiRequest request, String apiKey, boolean useThinking, int retryCount) {
        if (retryCount >= 3) {
            return new GeminiResponse(
                    null,
                    new GeminiError("MAX_RETRIES_EXCEEDED", "Failed after 3 retry attempts")
            );
        }

        try {
            // Estimate tokens before sending
            long estimatedTokens = tokenCounter.estimateTokens(request.getContents());
            if (!tokenCounter.canUseTokens(estimatedTokens)) {
                return new GeminiResponse(
                        null,
                        new GeminiError("TOKEN_LIMIT", "Insufficient daily token quota")
                );
            }

            String requestBody = buildRequestJson(request, useThinking);
            Response httpResponse = executeHttpRequest(apiKey, requestBody);

            if (httpResponse.isSuccessful()) {
                String responseBody = httpResponse.body().string();
                GeminiResponse geminiResponse = parseResponse(responseBody);
                tokenCounter.recordTokenUsage(estimatedTokens);
                return geminiResponse;
            } else if (httpResponse.code() == 429) {
                // Rate limited - wait and retry
                Thread.sleep(9000); // 9 second gap as specified
                return attemptRequest(request, apiKey, useThinking, retryCount + 1);
            } else {
                String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "";
                GeminiError error = parseErrorResponse(errorBody, httpResponse.code());
                return new GeminiResponse(null, error);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GeminiResponse(
                    null,
                    new GeminiError("INTERRUPTED", "Request was interrupted: " + e.getMessage())
            );
        } catch (Exception e) {
            return new GeminiResponse(
                    null,
                    new GeminiError("NETWORK_ERROR", "Failed to reach Gemini API: " + e.getMessage())
            );
        }
    }

    /**
     * Execute HTTP request to Gemini API
     */
    private Response executeHttpRequest(String apiKey, String requestBody) throws IOException {
        String url = GEMINI_API_URL + MODEL + ENDPOINT + "?key=" + apiKey;
        
        RequestBody body = RequestBody.create(
                requestBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        return httpClient.newCall(request).execute();
    }

    /**
     * Build request JSON with thinking mode support
     */
    private String buildRequestJson(GeminiRequest request, boolean useThinking) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"contents\": ").append(gson.toJson(request.getContents())).append(",");
        
        if (useThinking) {
            json.append("\"thinking\": {\"budgetTokens\": 10000},");
        }

        json.append("\"tools\": ").append(gson.toJson(request.getTools())).append(",");
        json.append("\"system_instruction\": ").append(gson.toJson(request.getSystemInstruction()));
        json.append("}");

        return json.toString();
    }

    /**
     * Parse Gemini API response
     */
    private GeminiResponse parseResponse(String responseBody) {
        try {
            GeminiResponse response = gson.fromJson(responseBody, GeminiResponse.class);
            return response;
        } catch (Exception e) {
            return new GeminiResponse(
                    null,
                    new GeminiError("PARSE_ERROR", "Failed to parse API response: " + e.getMessage())
            );
        }
    }

    /**
     * Parse error response from Gemini API
     */
    private GeminiError parseErrorResponse(String errorBody, int httpCode) {
        try {
            com.google.gson.JsonObject json = gson.fromJson(errorBody, com.google.gson.JsonObject.class);
            if (json.has("error")) {
                com.google.gson.JsonObject error = json.getAsJsonObject("error");
                String code = error.has("code") ? error.get("code").getAsString() : "UNKNOWN_ERROR";
                String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                return new GeminiError(code, message);
            }
        } catch (Exception e) {
            // Fallback error parsing
        }

        return new GeminiError("HTTP_ERROR", "HTTP " + httpCode + ": " + errorBody);
    }

    /**
     * Data classes for API communication
     */
    public static class GeminiRequest {
        private String systemInstruction;
        private Object contents;
        private Object tools;

        public GeminiRequest(String systemInstruction, Object contents, Object tools) {
            this.systemInstruction = systemInstruction;
            this.contents = contents;
            this.tools = tools;
        }

        public String getSystemInstruction() { return systemInstruction; }
        public Object getContents() { return contents; }
        public Object getTools() { return tools; }
    }

    public static class GeminiResponse {
        private Object candidates;
        private GeminiError error;

        public GeminiResponse(Object candidates, GeminiError error) {
            this.candidates = candidates;
            this.error = error;
        }

        public boolean isSuccess() { return error == null && candidates != null; }
        public Object getCandidates() { return candidates; }
        public GeminiError getError() { return error; }
    }

    public static class GeminiError {
        private String code;
        private String message;

        public GeminiError(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
