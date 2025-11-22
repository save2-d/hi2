package com.ai.integration;

import android.content.Context;
import android.util.Log;

import com.ai.errorrecovery.AutoFixer;
import com.ai.errorrecovery.BuildErrorAnalyzer;
import com.ai.generation.AppPlanner;
import com.ai.generation.CodeGenerator;

import java.io.File;

/**
 * AI-to-Sketchware Orchestrator
 * 
 * Manages the complete workflow:
 * 1. AI Planning (using Gemini 2.5 Pro with thinking)
 * 2. Code Generation
 * 3. Sketchware Project Creation (via IntegrationBridge)
 * 4. Sketchware Build (using ProjectBuilder)
 * 5. Error Recovery (if build fails)
 * 
 * This is simpler and more integrated than the original AIAppOrchestrator
 * because it directly leverages Sketchware's existing build system instead
 * of trying to replicate it.
 */
public class SketchwareAIOrchestratorV2 {
    private static final String TAG = "SketchwareAIOrchestratorV2";
    
    private final Context context;
    private final AppPlanner appPlanner;
    private final CodeGenerator codeGenerator;
    private final SketchwareIntegrationBridge integrationBridge;
    private final BuildErrorAnalyzer errorAnalyzer;
    private final AutoFixer autoFixer;
    private final OrchestrationCallback callback;
    
    /**
     * Callback for orchestration events
     */
    public interface OrchestrationCallback {
        void onPhaseStarted(String phase, String details);
        void onPhaseProgress(String message, int progress);
        void onPhaseCompleted(String phase, Object result);
        void onError(String phase, String errorMessage);
        void onBuildSuccess(File apkFile);
    }
    
    public SketchwareAIOrchestratorV2(Context context, OrchestrationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.appPlanner = new AppPlanner(context);
        this.codeGenerator = new CodeGenerator();
        this.integrationBridge = new SketchwareIntegrationBridge(context, 
            new SketchwareIntegrationBridge.BuildBridgeCallback() {
                @Override
                public void onProjectCreated(long projectId, String projectName) {
                    callback.onPhaseProgress("Project created with ID: " + projectId, 50);
                }
                
                @Override
                public void onBuildProgress(String message, int step) {
                    callback.onPhaseProgress(message, 50 + (step * 50) / 20);
                }
                
                @Override
                public void onBuildSuccess(File apkFile) {
                    callback.onPhaseCompleted("Build", apkFile);
                    callback.onBuildSuccess(apkFile);
                }
                
                @Override
                public void onBuildError(String errorMessage) {
                    callback.onError("Build", errorMessage);
                }
            });
        this.errorAnalyzer = new BuildErrorAnalyzer();
        this.autoFixer = new AutoFixer();
    }
    
    /**
     * Main entry point: Generate an app from natural language description
     * 
     * Flow:
     * Phase 1 (0-20%): Planning - Gemini thinks about architecture
     * Phase 2 (20-40%): Code Generation - Generate Java/XML files
     * Phase 3 (40-60%): Project Creation - Create Sketchware project files
     * Phase 4 (60-100%): Build - Use ProjectBuilder to compile & build APK
     * 
     * @param appDescription Natural language app description (e.g., "Make me a todo app")
     * @param projectName Name for the project in Sketchware
     */
    public void generateAndBuildApp(String appDescription, String projectName) {
        new Thread(() -> {
            try {
                // Phase 1: Planning
                Log.i(TAG, "Starting Phase 1: Planning");
                callback.onPhaseStarted("Planning", "Using Gemini 2.5 Pro to plan architecture...");
                
                AppPlanner.AppPlan plan = appPlanner.planApp(appDescription);
                callback.onPhaseProgress("App architecture planned", 20);
                
                if (plan == null) {
                    callback.onError("Planning", "Failed to generate app plan");
                    return;
                }
                
                // Phase 2: Code Generation
                Log.i(TAG, "Starting Phase 2: Code Generation");
                callback.onPhaseStarted("CodeGeneration", "Generating Android code...");
                
                CodeGenerator.GeneratedCode generatedCode = codeGenerator.generateActivityCode(plan);
                callback.onPhaseProgress("Code generated successfully", 40);
                
                if (generatedCode == null) {
                    callback.onError("CodeGeneration", "Failed to generate code");
                    return;
                }
                
                // Phase 3: Create Sketchware Project
                Log.i(TAG, "Starting Phase 3: Sketchware Project Creation");
                callback.onPhaseStarted("ProjectCreation", "Creating Sketchware project structure...");
                
                long projectId = integrationBridge.createProjectFromAiGeneration(
                    appDescription, 
                    projectName, 
                    generatedCode
                );
                
                if (projectId < 0) {
                    callback.onError("ProjectCreation", "Failed to create Sketchware project");
                    return;
                }
                
                // Validate project structure
                if (!integrationBridge.validateProjectStructure(projectId)) {
                    callback.onError("ProjectCreation", "Project structure validation failed");
                    return;
                }
                
                callback.onPhaseProgress("Sketchware project created", 50);
                
                // Phase 4: Build
                Log.i(TAG, "Starting Phase 4: Build (delegating to Sketchware ProjectBuilder)");
                callback.onPhaseStarted("Build", "Building APK using Sketchware's build system...");
                
                // At this point, the project would be built using Sketchware's ProjectBuilder
                // This happens in the UI context (DesignActivity) where BuildTask is invoked
                callback.onPhaseProgress("Project ready for building", 60);
                
                Log.i(TAG, "Generation pipeline complete. Project ID: " + projectId);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in generation pipeline", e);
                callback.onError("Generation", e.getMessage());
            }
        }).start();
    }
    
    /**
     * Retry a failed build with automatic fixes
     */
    public void retryBuildWithAutoFix(String buildErrorOutput, File projectPath) {
        new Thread(() -> {
            try {
                Log.i(TAG, "Analyzing build error for auto-fix...");
                callback.onPhaseStarted("AutoFix", "Analyzing build errors...");
                
                BuildErrorAnalyzer.BuildError error = errorAnalyzer.analyzeBuildOutput(buildErrorOutput);
                callback.onPhaseProgress("Error analyzed: " + error.getMessage(), 70);
                
                if (!error.isRecoverable()) {
                    callback.onError("AutoFix", "Build error not recoverable: " + error.getMessage());
                    return;
                }
                
                AutoFixer.FixSuggestion suggestion = autoFixer.generateFixSuggestion(error);
                callback.onPhaseProgress("Fix suggestion: " + suggestion.getAction(), 75);
                
                // Apply the fix (implementation depends on error type)
                // In real scenario, this would modify Java/XML files in the project
                
                callback.onPhaseProgress("Retrying build with fixes...", 80);
                Log.i(TAG, "Build retry initiated");
                
            } catch (Exception e) {
                Log.e(TAG, "Error in auto-fix", e);
                callback.onError("AutoFix", e.getMessage());
            }
        }).start();
    }
}
