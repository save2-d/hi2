package com.ai.orchestrator;

import android.content.Context;
import com.ai.appgenerator.AppPlanner;
import com.ai.appgenerator.CodeGenerator;
import com.ai.errorrecovery.AutoFixer;
import com.ai.errorrecovery.BuildErrorAnalyzer;
import com.ai.gemini.GeminiClient;
import com.ai.projectmanagement.SketchwareProjectAdapter;
import java.io.File;
import java.io.IOException;

/**
 * AI App Generation Orchestrator
 * Coordinates all AI modules to generate complete apps from natural language
 * 
 * Flow:
 * 1. User provides app description
 * 2. Gemini plans the app architecture (thinking enabled)
 * 3. Code generator creates Android code
 * 4. Sketchware adapter converts to Sketchware format
 * 5. GitHub Actions builds APK
 * 6. If build fails, AutoFixer analyzes and retries
 */
public class AIAppOrchestrator {
    private static final String TAG = "AIAppOrchestrator";
    private static final int MAX_AUTO_FIX_ATTEMPTS = 2;

    private final Context context;
    private final GeminiClient geminiClient;
    private final AppPlanner appPlanner;
    private final CodeGenerator codeGenerator;
    private final SketchwareProjectAdapter projectAdapter;
    private final BuildErrorAnalyzer errorAnalyzer;
    private final AutoFixer autoFixer;
    private final AppGenerationCallback callback;

    private AppPlanner.AppPlan currentPlan;
    private int buildAttemptCount = 0;

    public AIAppOrchestrator(Context context, AppGenerationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.geminiClient = new GeminiClient(context);
        this.appPlanner = new AppPlanner();
        this.codeGenerator = new CodeGenerator();
        this.projectAdapter = new SketchwareProjectAdapter(context);
        this.errorAnalyzer = new BuildErrorAnalyzer();
        this.autoFixer = new AutoFixer();
    }

    /**
     * Start complete app generation process
     */
    public void generateApp(String appDescription, String projectName) {
        new Thread(() -> {
            try {
                // Phase 1: Planning
                callback.onPhaseStarted("Planning", "Analyzing app requirements with AI thinking mode...");
                currentPlan = appPlanner.planApp(appDescription);
                callback.onPhaseCompleted("Planning", currentPlan);

                // Phase 2: Code Generation
                callback.onPhaseStarted("Code Generation", "Generating Android code...");
                CodeGenerator.GeneratedCode generatedCode = codeGenerator.generateActivityCode(
                        "MainActivity",
                        appDescription,
                        currentPlan
                );
                callback.onPhaseProgress("Code Generation", generatedCode.getFileCount() + " files generated");

                // Phase 3: Sketchware Adaptation
                callback.onPhaseStarted("Project Adaptation", "Converting to Sketchware format...");
                SketchwareProjectAdapter.ProjectContent content = new SketchwareProjectAdapter.ProjectContent();
                content.setPermissions(currentPlan.getPermissions());
                content.setDependencies(currentPlan.getDependencies().stream()
                        .map(dep -> dep.getName() + ":" + dep.getVersion())
                        .collect(java.util.stream.Collectors.toList()));

                File projectDir = projectAdapter.createProjectFromGenerated(projectName, content);
                callback.onPhaseCompleted("Project Adaptation", projectDir);

                // Phase 4: Build
                buildAttemptCount = 0;
                attemptBuild(projectName);

                callback.onGenerationSuccess(projectDir);

            } catch (Exception e) {
                callback.onGenerationError("Generation failed: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Attempt to build the APK
     */
    private void attemptBuild(String projectName) throws IOException {
        buildAttemptCount++;
        callback.onPhaseStarted("Build", "Building APK (Attempt " + buildAttemptCount + ")...");

        try {
            // This would trigger Gradle build
            // In real implementation, use ProcessBuilder or Gradle API
            callback.onPhaseCompleted("Build", new File("path/to/app.apk"));
        } catch (Exception e) {
            if (buildAttemptCount < MAX_AUTO_FIX_ATTEMPTS) {
                handleBuildError(e.getMessage());
                attemptBuild(projectName);
            } else {
                throw e;
            }
        }
    }

    /**
     * Handle build errors with auto-fix
     */
    private void handleBuildError(String buildOutput) {
        callback.onPhaseStarted("Error Recovery", "Analyzing build error...");

        BuildErrorAnalyzer.BuildError error = errorAnalyzer.analyzeBuildOutput(buildOutput);
        callback.onPhaseProgress("Error Recovery", "Error: " + error.getMessage());

        AutoFixer.FixSuggestion suggestion = autoFixer.generateFixSuggestion(error);
        AutoFixer.RetryRecommendation recommendation = autoFixer.getRetryRecommendation(buildAttemptCount, error);

        callback.onBuildError(error, suggestion, recommendation);
    }

    /**
     * Get current generation status
     */
    public GenerationStatus getStatus() {
        GenerationStatus status = new GenerationStatus();
        status.setCurrentPlan(currentPlan);
        status.setBuildAttemptCount(buildAttemptCount);
        status.setMaxAttempts(MAX_AUTO_FIX_ATTEMPTS);
        return status;
    }

    /**
     * Cancel ongoing generation
     */
    public void cancelGeneration() {
        callback.onGenerationCancelled("Generation cancelled by user");
    }

    /**
     * Callback interface for generation progress
     */
    public interface AppGenerationCallback {
        void onPhaseStarted(String phase, String description);
        void onPhaseProgress(String phase, String message);
        void onPhaseCompleted(String phase, Object result);
        void onBuildError(BuildErrorAnalyzer.BuildError error, AutoFixer.FixSuggestion suggestion, AutoFixer.RetryRecommendation recommendation);
        void onGenerationSuccess(File generatedProjectDir);
        void onGenerationError(String message, Exception exception);
        void onGenerationCancelled(String reason);
    }

    /**
     * Generation Status data class
     */
    public static class GenerationStatus {
        private AppPlanner.AppPlan currentPlan;
        private int buildAttemptCount;
        private int maxAttempts;

        public AppPlanner.AppPlan getCurrentPlan() { return currentPlan; }
        public void setCurrentPlan(AppPlanner.AppPlan currentPlan) { this.currentPlan = currentPlan; }

        public int getBuildAttemptCount() { return buildAttemptCount; }
        public void setBuildAttemptCount(int buildAttemptCount) { this.buildAttemptCount = buildAttemptCount; }

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    }
}
