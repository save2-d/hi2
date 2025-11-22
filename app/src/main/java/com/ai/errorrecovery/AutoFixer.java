package com.ai.errorrecovery;

/**
 * Auto-Fixer - Attempts to automatically fix common build errors
 * Works with BuildErrorAnalyzer to identify and repair issues
 */
public class AutoFixer {
    private static final String TAG = "AutoFixer";

    /**
     * Generate fix suggestion for a build error
     */
    public FixSuggestion generateFixSuggestion(BuildErrorAnalyzer.BuildError error) {
        FixSuggestion suggestion = new FixSuggestion();
        suggestion.setError(error);

        switch (error.getType()) {
            case SYNTAX_ERROR:
                suggestion.setFix("Check for missing semicolons, braces, or invalid syntax");
                suggestion.setAction(FixAction.REGENERATE_CLASS);
                break;

            case SYMBOL_NOT_FOUND:
                suggestion.setFix("Add missing import statement or check symbol name");
                suggestion.setAction(FixAction.ADD_IMPORT);
                break;

            case UNRESOLVED_REFERENCE:
                suggestion.setFix("Add missing dependency to build.gradle or import file");
                suggestion.setAction(FixAction.UPDATE_DEPENDENCIES);
                break;

            case DUPLICATE_DEFINITION:
                suggestion.setFix("Remove duplicate class or resource definition");
                suggestion.setAction(FixAction.REMOVE_DUPLICATE);
                break;

            case MANIFEST_ERROR:
                suggestion.setFix("Add missing permissions or fix manifest configuration");
                suggestion.setAction(FixAction.UPDATE_MANIFEST);
                break;

            case RESOURCE_ERROR:
                suggestion.setFix("Create missing layout, drawable, or resource file");
                suggestion.setAction(FixAction.CREATE_RESOURCE);
                break;

            case DEPENDENCY_ERROR:
                suggestion.setFix("Resolve version conflict in build.gradle");
                suggestion.setAction(FixAction.UPDATE_DEPENDENCIES);
                break;

            default:
                suggestion.setFix("Unknown error - manual review needed");
                suggestion.setAction(FixAction.MANUAL_REVIEW);
        }

        suggestion.setConfidence(calculateConfidence(error.getType()));

        return suggestion;
    }

    /**
     * Get retry recommendation
     */
    public RetryRecommendation getRetryRecommendation(int attemptCount, BuildErrorAnalyzer.BuildError lastError) {
        RetryRecommendation rec = new RetryRecommendation();

        if (attemptCount >= 2) {
            // After 2 attempts, ask user
            rec.setCanRetry(false);
            rec.setReason("Maximum auto-fix attempts reached. Please review the error and provide feedback.");
        } else if (!lastError.isRecoverable()) {
            rec.setCanRetry(false);
            rec.setReason("Error type is not automatically recoverable.");
        } else {
            rec.setCanRetry(true);
            rec.setReason("Attempting auto-fix...");
        }

        rec.setAttemptNumber(attemptCount + 1);

        return rec;
    }

    /**
     * Calculate fix confidence level (0-100)
     */
    private int calculateConfidence(BuildErrorAnalyzer.ErrorType type) {
        switch (type) {
            case SYNTAX_ERROR:
                return 60; // Can be tricky to auto-fix
            case SYMBOL_NOT_FOUND:
                return 70;
            case UNRESOLVED_REFERENCE:
                return 75;
            case DUPLICATE_DEFINITION:
                return 65;
            case MANIFEST_ERROR:
                return 80;
            case RESOURCE_ERROR:
                return 85;
            case DEPENDENCY_ERROR:
                return 70;
            default:
                return 30;
        }
    }

    /**
     * Fix Suggestion data class
     */
    public static class FixSuggestion {
        private BuildErrorAnalyzer.BuildError error;
        private String fix;
        private FixAction action;
        private int confidence;

        public BuildErrorAnalyzer.BuildError getError() { return error; }
        public void setError(BuildErrorAnalyzer.BuildError error) { this.error = error; }

        public String getFix() { return fix; }
        public void setFix(String fix) { this.fix = fix; }

        public FixAction getAction() { return action; }
        public void setAction(FixAction action) { this.action = action; }

        public int getConfidence() { return confidence; }
        public void setConfidence(int confidence) { this.confidence = confidence; }
    }

    /**
     * Retry Recommendation data class
     */
    public static class RetryRecommendation {
        private boolean canRetry;
        private String reason;
        private int attemptNumber;

        public boolean isCanRetry() { return canRetry; }
        public void setCanRetry(boolean canRetry) { this.canRetry = canRetry; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public int getAttemptNumber() { return attemptNumber; }
        public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    }

    /**
     * Fix actions that can be taken
     */
    public enum FixAction {
        REGENERATE_CLASS,       // Re-generate the entire class
        ADD_IMPORT,             // Add missing import
        UPDATE_DEPENDENCIES,    // Update build.gradle
        REMOVE_DUPLICATE,       // Remove duplicate definition
        UPDATE_MANIFEST,        // Update AndroidManifest.xml
        CREATE_RESOURCE,        // Create missing resource file
        MANUAL_REVIEW           // Needs manual review
    }
}
