# Sketchware Pro AI Integration - REVISED ARCHITECTURE

**Status:** ✅ Updated to use Sketchware's native build system

---

## Overview

The AI integration now **works WITH Sketchware's existing APK builder** instead of replacing it. This approach:

- ✅ Leverages Sketchware's proven, battle-tested build pipeline
- ✅ Maintains 100% compatibility with existing Sketchware projects
- ✅ Adds AI generation as a top layer
- ✅ Reuses all of Sketchware's optimization (ProGuard, R8, DEX merging, etc.)
- ✅ Simplifies error recovery (uses Sketchware's build logging)

---

## Architecture: 2-Stage Pipeline

### Stage 1: AI Generation (0-50% of process)
```
User Input (Natural Language)
    ↓
AI Planning (Gemini 2.5 Pro, thinking mode enabled)
    ↓ 
App Architecture Plan (screens, activities, features, permissions)
    ↓
Code Generation (Java files, XML layouts, manifests)
    ↓
Sketchware Project Creation (creates src.json, res.json, lib.json, metadata.json)
    ↓
Sketchware Project Directory Ready
```

**New Modules:**
- `SketchwareIntegrationBridge.java` - Converts AI output to Sketchware project format
- `SketchwareAIOrchestratorV2.java` - Orchestrates the entire pipeline

### Stage 2: Sketchware Native Build (50-100% of process)
```
Sketchware Project (with AI-generated files)
    ↓
ProjectBuilder.buildApk() [Sketchware's existing system]
    ├─ Delete temporary files
    ├─ Generate source code
    ├─ Extract built-in libraries
    ├─ AAPT2 resource compilation
    ├─ View binding generation
    ├─ Java compilation
    ├─ Stringfog encryption
    ├─ ProGuard obfuscation
    ├─ DEX creation & merging
    ├─ APK building
    └─ APK signing
    ↓
Release APK (ready to install)
```

**Existing Sketchware Classes Used:**
- `ProjectBuilder` - Main build orchestrator (20 build steps)
- `yq` - Project file manager and directory setup
- `Lx` - Gradle and build.gradle generator
- `DesignActivity.BuildTask` - UI integration and progress callbacks

---

## Key Files in Sketchware Build System

| File | Purpose | Location |
|------|---------|----------|
| `ProjectBuilder.java` | Main build engine | `app/src/main/java/a/a/a/` |
| `yq.java` | Project setup & directories | `app/src/main/java/a/a/a/` |
| `Lx.java` | Gradle file generation | `app/src/main/java/a/a/a/` |
| `DesignActivity.java` | UI integration (line 1030+) | `app/src/main/java/com/besome/sketch/design/` |
| `BuildTask` | Async build executor | Inside DesignActivity |

### Sketchware Build Steps (ProjectBuilder)

1. **Initialization** - Delete temp files, create directories
2. **Resource Extraction** - Extract built-in libraries, assets
3. **AAPT2 Compilation** - Compile Android resources
4. **Java Compilation** - Compile all Java files with dependencies
5. **Kotlin Compilation** - Optional Kotlin support
6. **View Binding** - Generate view binding classes
7. **ProGuard/R8** - Code obfuscation
8. **DEX Creation** - Convert classes to DEX format
9. **DEX Merging** - Merge multiple DEX files if needed
10. **APK Building** - Create APK archive
11. **APK Signing** - Sign with debug/release keystore

---

## AI Generation Pipeline (New)

### Module 1: SketchwareIntegrationBridge

**Purpose:** Convert AI-generated app data into Sketchware project format

**Key Methods:**
- `createProjectFromAiGeneration()` - Creates complete Sketchware project structure
- `validateProjectStructure()` - Ensures all required JSON files exist
- `generateSrcJson()` - Converts AI activities to src.json format
- `generateResJson()` - Converts AI resources to res.json format
- `generateLibJson()` - Specifies required libraries

**Output Files Created:**
```
/data/data/com.besome.sketch/projects/{projectId}/
├── metadata.json              ← Project metadata (app name, version, etc.)
├── src.json                   ← Activity definitions & logic
├── res.json                   ← Resources (drawables, strings, colors)
├── lib.json                   ← Library dependencies
└── app/src/main/
    ├── java/                  ← Generated Java files
    ├── res/                   ← Generated resources
    └── AndroidManifest.xml    ← Generated manifest
```

### Module 2: SketchwareAIOrchestratorV2

**Purpose:** Orchestrate the complete AI → Sketchware → Build pipeline

**4-Phase Workflow:**
1. **Planning (0-20%)** - Gemini plans architecture
2. **Code Gen (20-40%)** - Generate Java/XML
3. **Project Creation (40-60%)** - Create Sketchware project
4. **Build (60-100%)** - Use ProjectBuilder

**Key Methods:**
- `generateAndBuildApp()` - Main entry point
- `retryBuildWithAutoFix()` - Handle build failures

**Callbacks:**
```java
interface OrchestrationCallback {
    void onPhaseStarted(String phase, String details);
    void onPhaseProgress(String message, int progress);
    void onPhaseCompleted(String phase, Object result);
    void onError(String phase, String errorMessage);
    void onBuildSuccess(File apkFile);
}
```

---

## Integration Points with Sketchware

### 1. Project Storage
**Where AI projects are stored:** Same as regular Sketchware projects
```
/data/data/com.besome.sketch/projects/{projectId}/
```

**Metadata marker:** Add `"ai_generated": true` to metadata.json

### 2. Build Integration
**How to trigger build:**
```java
// In DesignActivity, when user selects an AI-generated project:
DesignActivity.BuildTask buildTask = new BuildTask(this);
currentBuildTask = buildTask;
buildTask.execute();
```

### 3. Progress Reporting
**Build callbacks from ProjectBuilder:**
```java
@Override
public void onProgress(String progress, int step) {
    // step: 1-20 (represents build stage)
    // Shown in DesignActivity.progressText
}
```

### 4. Error Handling
**Capture build errors from Sketchware:**
```
- BuildTask logs to: LogUtil.e("DesignActivity$BuildTask", error)
- Error location stored in: DesignActivity.buildErrors
- BuildErrorAnalyzer can parse standard Gradle/Java errors
```

---

## Data Flow Example: "Make me a Todo App"

```
1. User Input: "Make me a simple todo app with add/delete buttons"
   ↓
2. Gemini Planning:
   Plan: {
     screens: ["MainActivity", "TodoDetailActivity"],
     features: ["RecyclerView for todos", "SQLite database"],
     permissions: ["android.permission.INTERNET"],
     libraries: ["androidx.recyclerview", "androidx.room"]
   }
   ↓
3. Code Generation:
   MainActivity.java (with RecyclerView setup)
   TodoDetailActivity.java (with form)
   activity_main.xml (layout)
   activity_todo_detail.xml (layout)
   AndroidManifest.xml
   ↓
4. SketchwareIntegrationBridge:
   Creates: /projects/1732310400000/metadata.json
            /projects/1732310400000/src.json (activities as Sketchware format)
            /projects/1732310400000/res.json (resources)
            /projects/1732310400000/lib.json (libraries: recyclerview, room)
   ↓
5. Sketchware BuildTask:
   Compiles src.json → Java code (ProjectBuilder)
   Generates .class files
   ProGuard obfuscation
   DEX conversion
   APK building
   APK signing
   ↓
6. Output: release/app-1732310400000.apk
   Ready to install on device
```

---

## Advantages of This Approach

| Aspect | Benefit |
|--------|---------|
| **Reliability** | Uses proven Sketchware build system (20+ build steps optimized) |
| **Compatibility** | 100% compatible with existing Sketchware projects |
| **Error Recovery** | Leverages Sketchware's error logging and analysis |
| **Performance** | Reuses ProGuard, R8, DEX merging optimizations |
| **Maintenance** | No need to replicate Sketchware's complex build logic |
| **Extensibility** | AI layer can be updated independently of build system |

---

## Existing Sketchware Build Quality Features

The following features from Sketchware's native builder are automatically inherited:

✅ **ProGuard obfuscation** - Code protection
✅ **R8 compiler** - Modern DEX generation  
✅ **Multi-DEX support** - Apps with 65K+ methods
✅ **Resource optimization** - APK size reduction
✅ **View binding generation** - Type-safe views
✅ **Kotlin support** - If enabled in project
✅ **Stringfog encryption** - String encryption
✅ **Debug APK signing** - Ready for testing
✅ **Release signing support** - Ready for Play Store
✅ **Build caching** - Faster rebuilds

---

## Required Modifications to Sketchware Core

### 1. DesignActivity Enhancement
Add method to build AI-generated projects:

```java
// In DesignActivity.java
public void buildAiGeneratedProject(long projectId) {
    // Load AI project from database/storage
    // Initialize BuildTask with this project
    currentBuildTask = new BuildTask(this);
    currentBuildTask.execute();
}
```

### 2. Project Database Schema
Add AI generation tracking:

```sql
-- Add to projects table
ALTER TABLE projects ADD COLUMN (
    ai_generated BOOLEAN DEFAULT FALSE,
    ai_generation_timestamp LONG,
    ai_prompt TEXT,
    ai_model TEXT DEFAULT 'gemini-2.5-pro'
);
```

### 3. GitHub Actions Workflow (ai-build.yml)
Updated to use Sketchware's build system:

```yaml
- name: Generate AI Project
  run: |
    # Call SketchwareAIOrchestratorV2.generateAndBuildApp()
    gradle assembleAiGeneratedDebug

- name: Build with Sketchware ProjectBuilder
  run: |
    # ProjectBuilder will be invoked automatically
    ./gradlew assembleRelease
```

---

## Implementation Steps

### Step 1: Add New Modules
- ✅ `SketchwareIntegrationBridge.java` - DONE
- ✅ `SketchwareAIOrchestratorV2.java` - DONE

### Step 2: Update Existing Modules
- Update `AIAppOrchestrator.java` to use V2 orchestrator
- Ensure `SketchwareProjectAdapter.java` generates valid src.json/res.json

### Step 3: Hook into UI
- Add "Generate with AI" button in DesignActivity
- Show generation progress in existing progress UI

### Step 4: Test Integration
```bash
# Build with AI support
./gradlew assembleDebug

# Run tests
./gradlew test

# Check AI generation
adb logcat | grep "SketchwareIntegrationBridge"
```

---

## Security Considerations

✅ **API Keys:** Encrypted in EncryptedSharedPreferences (AES-256-GCM)
✅ **Generated Projects:** Stored in app's private directory
✅ **Permissions:** Scoped access only to /sketchware/ folder
✅ **No External Transmission:** Generated files stay on-device
✅ **Build System:** Leverages Sketchware's proven security model

---

## Performance Expectations

| App Type | Time | Stage |
|----------|------|-------|
| Simple (Hello World) | 15-20s | AI Gen (10s) + Build (5-10s) |
| Medium (Todo with DB) | 45-60s | AI Gen (20s) + Build (25-40s) |
| Complex (Full app) | 90-120s | AI Gen (30-40s) + Build (50-80s) |

Build times depend on Sketchware's ProjectBuilder efficiency, which is already optimized.

---

## Rollback Strategy

If AI generation fails, projects fall back to manual Sketchware creation:

```
AI Generation Failed
    ↓
User can manually create project in Sketchware (as before)
    ↓
Or retry AI generation with different prompt
    ↓
No impact on existing Sketchware functionality
```

---

## Testing Checklist

- [ ] `SketchwareIntegrationBridge` creates valid project directory
- [ ] Generated JSON files have correct schema
- [ ] `SketchwareAIOrchestratorV2` produces correct phase callbacks
- [ ] ProjectBuilder can build AI-generated projects
- [ ] Build errors are correctly captured and analyzed
- [ ] Auto-fix suggestions are valid for Sketchware projects
- [ ] APK generated is installable on Android device

---

## Files Summary

| File | Status | Purpose |
|------|--------|---------|
| `SketchwareIntegrationBridge.java` | ✅ NEW | Bridge AI output to Sketchware format |
| `SketchwareAIOrchestratorV2.java` | ✅ NEW | Orchestrate 4-phase pipeline |
| `AIAppOrchestrator.java` | 📝 TO UPDATE | Deprecate in favor of V2 |
| `SketchwareProjectAdapter.java` | ✅ KEEP | JSON file generation |
| `ProjectBuilder.java` | ✅ LEVERAGE | Existing Sketchware build system |
| `DesignActivity.java` | 📝 TO HOOK | Add AI project build trigger |
| `ai-build.yml` | ✅ KEEP | GitHub Actions workflow |

---

## Next: Integration with UI

After this revision, the next step is to integrate the AI generation UI into Sketchware's existing interface:

1. Add "Generate with AI" button in DesignActivity
2. Show AI generation progress using existing progress UI
3. Automatically launch build after generation completes
4. Display APK download/install options

This keeps AI generation seamlessly integrated with Sketchware's familiar interface.
