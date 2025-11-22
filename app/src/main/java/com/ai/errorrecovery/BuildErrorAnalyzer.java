package com.ai.errorrecovery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build Error Analyzer
 * Parses Gradle/Android build errors and extracts actionable information
 * Used by AutoFixer to determine what went wrong and how to fix it
 */
public class BuildErrorAnalyzer {
    private static final String TAG = "BuildErrorAnalyzer";

    /**
     * Analyze build error output
     */
    public BuildError analyzeBuildOutput(String buildOutput) {
        BuildError error = new BuildError();

        if (buildOutput == null || buildOutput.isEmpty()) {
            error.setType(ErrorType.UNKNOWN);
            error.setMessage("Build output is empty");
            return error;
        }

        String lowerOutput = buildOutput.toLowerCase();

        // Check for common error patterns
        if (lowerOutput.contains("not found") || lowerOutput.contains("cannot find symbol")) {
            error.setType(ErrorType.SYMBOL_NOT_FOUND);
            extractSymbolError(buildOutput, error);
        } else if (lowerOutput.contains("cannot resolve") || lowerOutput.contains("unresolved reference")) {
            error.setType(ErrorType.UNRESOLVED_REFERENCE);
            extractUnresolvedReference(buildOutput, error);
        } else if (lowerOutput.contains("duplicate") || lowerOutput.contains("already defined")) {
            error.setType(ErrorType.DUPLICATE_DEFINITION);
            extractDuplicateError(buildOutput, error);
        } else if (lowerOutput.contains("syntax error") || lowerOutput.contains("unexpected token")) {
            error.setType(ErrorType.SYNTAX_ERROR);
            extractSyntaxError(buildOutput, error);
        } else if (lowerOutput.contains("permission") || lowerOutput.contains("manifest")) {
            error.setType(ErrorType.MANIFEST_ERROR);
            extractManifestError(buildOutput, error);
        } else if (lowerOutput.contains("resource") || lowerOutput.contains("@")) {
            error.setType(ErrorType.RESOURCE_ERROR);
            extractResourceError(buildOutput, error);
        } else if (lowerOutput.contains("dependency") || lowerOutput.contains("version conflict")) {
            error.setType(ErrorType.DEPENDENCY_ERROR);
            extractDependencyError(buildOutput, error);
        } else {
            error.setType(ErrorType.UNKNOWN);
            error.setMessage(buildOutput.substring(0, Math.min(200, buildOutput.length())));
        }

        error.setSeverity(calculateSeverity(error.getType()));
        error.setRecoverable(isRecoverable(error.getType()));

        return error;
    }

    /**
     * Extract symbol not found error details
     */
    private void extractSymbolError(String output, BuildError error) {
        Pattern pattern = Pattern.compile("cannot find symbol.*?symbol:\\s*(\\w+)");
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            String symbol = matcher.group(1);
            error.setMessage("Symbol '" + symbol + "' not found");
            error.setDetails("Missing import or undefined symbol: " + symbol);
        }
    }

    /**
     * Extract unresolved reference error details
     */
    private void extractUnresolvedReference(String output, BuildError error) {
        Pattern pattern = Pattern.compile("unresolved reference.*?['\"]?([\\w.]+)['\"]?");
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            String reference = matcher.group(1);
            error.setMessage("Unresolved reference: " + reference);
            error.setDetails("Likely missing dependency or import");
        }
    }

    /**
     * Extract duplicate definition error
     */
    private void extractDuplicateError(String output, BuildError error) {
        Pattern pattern = Pattern.compile("duplicate.*?entry\\s*['\"]?([\\w.]+)['\"]?");
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            String entry = matcher.group(1);
            error.setMessage("Duplicate definition: " + entry);
            error.setDetails("Class or resource already defined elsewhere");
        }
    }

    /**
     * Extract syntax error details
     */
    private void extractSyntaxError(String output, BuildError error) {
        Pattern pattern = Pattern.compile(":(\\d+):\\s*error:\\s*(.+)");
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            String line = matcher.group(1);
            String message = matcher.group(2);
            error.setMessage("Syntax error at line " + line);
            error.setDetails(message);
        } else {
            error.setMessage("Syntax error");
            error.setDetails("Check for missing braces, semicolons, or invalid syntax");
        }
    }

    /**
     * Extract manifest error details
     */
    private void extractManifestError(String output, BuildError error) {
        if (output.toLowerCase().contains("permission")) {
            error.setMessage("Manifest permission error");
            error.setDetails("Missing permission declaration in AndroidManifest.xml");
        } else {
            error.setMessage("AndroidManifest.xml error");
            error.setDetails("Invalid manifest configuration");
        }
    }

    /**
     * Extract resource error details
     */
    private void extractResourceError(String output, BuildError error) {
        Pattern pattern = Pattern.compile("[Rr]esource.*?['\"]?([\\w/.]+)['\"]?.*?not");
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            String resource = matcher.group(1);
            error.setMessage("Resource not found: " + resource);
            error.setDetails("Missing layout, drawable, or resource file");
        }
    }

    /**
     * Extract dependency error details
     */
    private void extractDependencyError(String output, BuildError error) {
        error.setMessage("Dependency resolution error");
        error.setDetails("Version conflict or missing dependency in build.gradle");
    }

    /**
     * Calculate error severity (CRITICAL, HIGH, MEDIUM, LOW)
     */
    private String calculateSeverity(ErrorType type) {
        switch (type) {
            case SYNTAX_ERROR:
            case DUPLICATE_DEFINITION:
                return "CRITICAL";
            case SYMBOL_NOT_FOUND:
            case UNRESOLVED_REFERENCE:
                return "HIGH";
            case RESOURCE_ERROR:
            case MANIFEST_ERROR:
                return "MEDIUM";
            case DEPENDENCY_ERROR:
                return "HIGH";
            default:
                return "MEDIUM";
        }
    }

    /**
     * Check if error is recoverable by AI
     */
    private boolean isRecoverable(ErrorType type) {
        switch (type) {
            case SYNTAX_ERROR:
            case SYMBOL_NOT_FOUND:
            case UNRESOLVED_REFERENCE:
            case RESOURCE_ERROR:
                return true;
            case DUPLICATE_DEFINITION:
            case DEPENDENCY_ERROR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Build Error data class
     */
    public static class BuildError {
        private ErrorType type;
        private String message;
        private String details;
        private String severity;
        private boolean recoverable;

        public ErrorType getType() { return type; }
        public void setType(ErrorType type) { this.type = type; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public boolean isRecoverable() { return recoverable; }
        public void setRecoverable(boolean recoverable) { this.recoverable = recoverable; }

        @Override
        public String toString() {
            return "BuildError{" +
                    "type=" + type +
                    ", message='" + message + '\'' +
                    ", severity='" + severity + '\'' +
                    ", recoverable=" + recoverable +
                    '}';
        }
    }

    /**
     * Error types
     */
    public enum ErrorType {
        SYNTAX_ERROR,
        SYMBOL_NOT_FOUND,
        UNRESOLVED_REFERENCE,
        DUPLICATE_DEFINITION,
        MANIFEST_ERROR,
        RESOURCE_ERROR,
        DEPENDENCY_ERROR,
        UNKNOWN
    }
}
