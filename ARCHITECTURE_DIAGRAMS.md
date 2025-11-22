# System Architecture Diagrams

## 1. High-Level Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                      USER INPUT                             │
│         "Make me a calculator with memory"                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────▼────────────────────┐
        │    SketchwareAIOrchestratorV2         │
        │  (Manages 4-phase workflow)           │
        └──────────────────┬────────────────────┘
                           │
        ┌──────────────────▼─────────────────────┐
        │ PHASE 1: PLANNING (0-20%)              │
        │ ┌─────────────────────────────────────┐│
        │ │  AppPlanner + Gemini 2.5 Pro       ││
        │ │  (Thinking mode enabled)            ││
        │ │  Output: App architecture plan      ││
        │ └─────────────────────────────────────┘│
        └──────────────────┬─────────────────────┘
                           │
        ┌──────────────────▼─────────────────────┐
        │ PHASE 2: CODE GENERATION (20-40%)      │
        │ ┌─────────────────────────────────────┐│
        │ │  CodeGenerator                      ││
        │ │  Output:                            ││
        │ │  - MainActivity.java                ││
        │ │  - activity_main.xml                ││
        │ │  - AndroidManifest.xml              ││
        │ └─────────────────────────────────────┘│
        └──────────────────┬─────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────┐
        │ PHASE 3: PROJECT CREATION (40-60%)         │
        │ ┌──────────────────────────────────────────┐│
        │ │  SketchwareIntegrationBridge            ││
        │ │  Converts code → Sketchware JSON format ││
        │ │  Output:                                ││
        │ │  /projects/{projectId}/                ││
        │ │  ├── metadata.json                      ││
        │ │  ├── src.json                           ││
        │ │  ├── res.json                           ││
        │ │  └── lib.json                           ││
        │ └──────────────────────────────────────────┘│
        └──────────────────┬──────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────┐
        │ PHASE 4: BUILD (60-100%)                   │
        │ ┌──────────────────────────────────────────┐│
        │ │  Sketchware ProjectBuilder               ││
        │ │  (20+ build steps)                       ││
        │ │  ├─ Resource compilation (AAPT2)        ││
        │ │  ├─ Java compilation                    ││
        │ │  ├─ ProGuard obfuscation                ││
        │ │  ├─ DEX creation & merge                ││
        │ │  └─ APK building & signing              ││
        │ │  Output: app-release.apk                ││
        │ └──────────────────────────────────────────┘│
        └──────────────────┬──────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────┐
        │             APK READY ✅                │
        │   calculator-20250101.apk (2.1 MB)     │
        │  Ready to install on Android device    │
        └───────────────────────────────────────┘
```

---

## 2. Module Interaction Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                   SKETCHWARE PRO                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         AI Generation Layer                         │   │
│  │  ┌────────────────────────────────────────────────┐ │   │
│  │  │  GeminiClient ← → Gemini 2.5 Pro API          │ │   │
│  │  │                                                │ │   │
│  │  │  + APIKeyManager (encryption)                 │ │   │
│  │  │  + RateLimitHandler (1.8 RPM)                 │ │   │
│  │  │  + TokenCounter (quota management)            │ │   │
│  │  └────────────────────────────────────────────────┘ │   │
│  │                                                      │   │
│  │  ┌────────────────────────────────────────────────┐ │   │
│  │  │  AppPlanner (Architecture planning)            │ │   │
│  │  │  ↓ (generates AppPlan)                         │ │   │
│  │  │  CodeGenerator (Java/XML code)                │ │   │
│  │  │  ↓ (generates GeneratedCode)                  │ │   │
│  │  └────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                       ↓                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │      Integration & Orchestration Layer              │   │
│  │  ┌────────────────────────────────────────────────┐ │   │
│  │  │  SketchwareIntegrationBridge                  │ │   │
│  │  │  (Converts code → Sketchware JSON)            │ │   │
│  │  │  ├─ generateSrcJson()                         │ │   │
│  │  │  ├─ generateResJson()                         │ │   │
│  │  │  ├─ generateLibJson()                         │ │   │
│  │  │  └─ createProjectFromAiGeneration()           │ │   │
│  │  └────────────────────────────────────────────────┘ │   │
│  │                       ↓                              │   │
│  │  ┌────────────────────────────────────────────────┐ │   │
│  │  │  SketchwareAIOrchestratorV2                   │ │   │
│  │  │  (Manages 4-phase pipeline)                   │ │   │
│  │  │  generateAndBuildApp()                        │ │   │
│  │  └────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                       ↓                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │      Native Build Layer (Sketchware)               │   │
│  │  ┌────────────────────────────────────────────────┐ │   │
│  │  │  ProjectBuilder (existing Sketchware)          │ │   │
│  │  │  ├─ compileResources() [AAPT2]                │ │   │
│  │  │  ├─ compileJavaCode() [ECJ]                   │ │   │
│  │  │  ├─ createDexFilesFromClasses() [D8]          │ │   │
│  │  │  ├─ getDexFilesReady() [merge]                │ │   │
│  │  │  ├─ buildApk() [AAPT]                         │ │   │
│  │  │  └─ signDebugApk() [jarsigner]                │ │   │
│  │  │                                                │ │   │
│  │  │  yq (project file management)                 │ │   │
│  │  │  Lx (Gradle generation)                       │ │   │
│  │  └────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                       ↓                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │      Error Recovery Layer                          │   │
│  │  ┌────────────────────────────────────────────────┐ │   │
│  │  │  BuildErrorAnalyzer                            │ │   │
│  │  │  (Parse & classify 8 error types)              │ │   │
│  │  │          ↓                                       │ │   │
│  │  │  AutoFixer                                     │ │   │
│  │  │  (Generate fixes & retry suggestions)          │ │   │
│  │  └────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                       ↓                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │      UI Layer                                      │   │
│  │  ┌────────────────────────────────────────────────┐ │   │
│  │  │  DesignActivity                                │ │   │
│  │  │  ├─ "Generate with AI" button                 │ │   │
│  │  │  ├─ Progress display                          │ │   │
│  │  │  ├─ Install/Share APK                         │ │   │
│  │  │  └─ Error handling UI                         │ │   │
│  │  │                                                │ │   │
│  │  │  APIKeySettingsActivity                       │ │   │
│  │  │  └─ Manage encrypted API keys                 │ │   │
│  │  └────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. Data Structure Flow

```
User Input: "Make me a todo app"
     │
     ▼
AppPlan {
  screens: [
    { name: "MainActivity", title: "Todo List" },
    { name: "DetailActivity", title: "Add/Edit" }
  ],
  features: ["RecyclerView", "SQLite DB", "Search"],
  permissions: ["INTERNET"],
  libraries: ["androidx.recyclerview", "androidx.room"]
}
     │
     ▼
GeneratedCode {
  activities: {
    "MainActivity.java": "public class MainActivity...",
    "DetailActivity.java": "..."
  },
  layouts: {
    "activity_main.xml": "<?xml...>",
    "activity_detail.xml": "..."
  },
  manifest: "<?xml...>",
  resources: {...}
}
     │
     ▼
SketchwareProject (/projects/{projectId}/) {
  metadata.json: {
    app_name: "TodoApp",
    package_name: "com.ai.generated.1234567890",
    version_code: 1,
    version_name: "1.0",
    ai_generated: true,
    target_sdk: 34,
    min_sdk: 21
  },
  src.json: {
    activities: [...],
    components: [...],
    events: [...],
    logic: [...]
  },
  res.json: {
    strings: {...},
    colors: {...},
    drawables: {...}
  },
  lib.json: {
    libraries: ["androidx.recyclerview", "androidx.room"]
  }
}
     │
     ▼
[ProjectBuilder builds from above]
     │
     ▼
APK File {
  name: "app-release.apk",
  size: "2.3 MB",
  location: "/app/build/outputs/apk/release/",
  ready: true
}
     │
     ▼
Ready to Install/Share ✅
```

---

## 4. Build Pipeline Steps

```
ProjectBuilder.buildApk() [Total: 20 steps, ~30-60 seconds]

Step 1: Delete temporary files
        └─ Clean workspace

Step 2: Generate source code
        └─ Parse JSON, create Java files

Step 3: Extract built-in libraries
        ├─ AppCompat
        ├─ AndroidX
        └─ Other SDKs

Step 4-5: Resource Compilation (AAPT2)
        ├─ Compile layouts
        ├─ Compile drawables
        ├─ Generate R.java
        └─ Create APK resources

Step 6: Generate View Binding (optional)
        └─ Type-safe view references

Step 7: Kotlin Compilation (if enabled)
        └─ Convert Kotlin → Java bytecode

Step 8: Java Compilation (ECJ - Eclipse Compiler)
        ├─ Compile all .java files
        ├─ Include dependencies
        └─ Generate .class files

Step 9-10: StringFog + ProGuard
        ├─ Encrypt strings (StringFog)
        └─ Obfuscate code (ProGuard)

Step 11: R8 Compiler
        ├─ Code shrinking
        ├─ Resource optimization
        └─ Advanced obfuscation

Step 12: DEX Creation (D8)
        ├─ Convert .class → .dex
        ├─ Handle 65K+ method limit
        └─ Create DEX files

Step 13: DEX Merging
        ├─ If multi-DEX: merge DEX files
        └─ Optimize for runtime

Step 14-15: APK Building (AAPT)
        ├─ Create APK structure
        ├─ Add resources
        ├─ Add DEX files
        └─ Add assets

Step 16-17: APK Signing
        ├─ Load keystore
        ├─ Sign with private key
        └─ Add signature block

Step 18-20: Finalization
        ├─ Align APK (4-byte)
        ├─ Optimize compression
        └─ Generate manifest

Result: ✅ Release APK ready to install
```

---

## 5. Error Recovery Flow

```
Build Command
     │
     ├─ SUCCESS ──────────────────────────┐
     │                                    │
     └─ FAILURE                           │
           │                              │
           ▼                              │
     [Error logged]                       │
           │                              │
           ▼                              │
     BuildErrorAnalyzer                   │
     ├─ Parse error output                │
     ├─ Classify error type (8 types)     │
     │  ├─ SYNTAX_ERROR                   │
     │  ├─ SYMBOL_NOT_FOUND               │
     │  ├─ UNRESOLVED_REFERENCE           │
     │  ├─ DUPLICATE_DEFINITION           │
     │  ├─ MANIFEST_ERROR                 │
     │  ├─ RESOURCE_ERROR                 │
     │  ├─ DEPENDENCY_ERROR               │
     │  └─ UNKNOWN                        │
     │                                    │
     └─ Determine severity                │
        ├─ CRITICAL (don't retry)         │
        ├─ HIGH (try to fix)              │
        ├─ MEDIUM (fix & retry)           │
        └─ LOW (can retry)                │
                 │                        │
                 ▼                        │
            AutoFixer                     │
            ├─ Generate fix suggestion    │
            ├─ Calculate confidence       │
            │  └─ 30-85% based on type    │
            │                             │
            ├─ Suggest retry strategy     │
            │  ├─ Regenerate class        │
            │  ├─ Add import              │
            │  ├─ Update dependency       │
            │  ├─ Remove duplicate        │
            │  ├─ Update manifest         │
            │  ├─ Create resource         │
            │  └─ Manual review (fallback)│
            │                             │
            └─ Attempt fix (max 2x)       │
                 │                        │
                 ├─ FIX SUCCESS ──────────┼─────────────┐
                 │                        │             │
                 └─ FIX FAILED            │             │
                      │                   │             │
                      ▼                   │             │
                Show error to user        │             │
                - Error message           │             │
                - Suggested fix           │             │
                - Manual instructions     │             │
                                          │             │
                                          ▼             ▼
                                    [Report Success]
                                          │
                                          ▼
                                    Install APK
```

---

## 6. File System Structure

```
Sketchware Project Storage
/data/data/com.besome.sketch/
│
├── projects/
│   │
│   └── {projectId}/ ← AI-generated project
│       │
│       ├── metadata.json
│       │   ├── app_name: "TodoApp"
│       │   ├── package_name: "com.ai.generated.1234"
│       │   ├── version: "1.0"
│       │   ├── ai_generated: true
│       │   ├── target_sdk: 34
│       │   └── min_sdk: 21
│       │
│       ├── src.json
│       │   ├── activities: [...]
│       │   ├── components: [...]
│       │   ├── events: [...]
│       │   └── logic: [...]
│       │
│       ├── res.json
│       │   ├── strings: {...}
│       │   ├── colors: {...}
│       │   ├── drawables: {...}
│       │   └── dimensions: {...}
│       │
│       ├── lib.json
│       │   ├── libraries: [...]
│       │   ├── builtin: [...]
│       │   └── external: [...]
│       │
│       └── app/src/main/
│           ├── java/
│           │   └── com/ai/generated/{projectId}/
│           │       ├── MainActivity.java
│           │       ├── DetailActivity.java
│           │       └── helpers/
│           │
│           ├── res/
│           │   ├── layout/
│           │   │   ├── activity_main.xml
│           │   │   ├── activity_detail.xml
│           │   │   └── item_todo.xml
│           │   │
│           │   ├── values/
│           │   │   ├── colors.xml
│           │   │   ├── strings.xml
│           │   │   └── dimens.xml
│           │   │
│           │   ├── drawable/
│           │   │   ├── app_icon.png
│           │   │   └── ic_launcher.png
│           │   │
│           │   └── menu/
│           │       └── menu_main.xml
│           │
│           └── AndroidManifest.xml
│
├── cache/
│   └── (Sketchware's build cache)
│
└── apks/
    └── app-release-1234567890.apk ← Final output
```

---

## 7. Integration Points with Sketchware

```
Sketchware Existing Code
├── DesignActivity.java
│   ├── onCreate() ─────────────┐
│   ├── onOptionsItemSelected()─│─→ Add AI button here
│   ├── BuildTask ──────────────┼─→ Can use existing progress UI
│   └── projectId ──────────────┘
│
├── ProjectBuilder.java
│   ├── buildApk() ─────────────────→ We call this
│   ├── compileResources() ─────────→ Sketchware handles
│   ├── compileJavaCode() ──────────→ Sketchware handles
│   └── signDebugApk() ─────────────→ Sketchware handles
│
├── yq.java (Project paths)
│   └── projectMyscPath ────────────→ We use this for file creation
│
└── Lx.java (Gradle generation)
    └── getBuildGradleString() ─────→ Sketchware uses this
```

---

## 8. Performance Breakdown

```
Timeline for "Make me a Todo App"

0s      ┌─────────────────────────────────────────┐
        │ Start                                   │
1s      ├─────────────────────────────────────────┤
        │ Initialize Gemini API                  │
3s      ├─────────────────────────────────────────┤
        │ Plan architecture (thinking mode)      │ ← Gemini
8s      ├─────────────────────────────────────────┤
        │ Code generation                        │ ← CodeGenerator
12s     ├─────────────────────────────────────────┤
        │ Create Sketchware project files        │ ← IntegrationBridge
14s     ├─────────────────────────────────────────┤
        │ Start ProjectBuilder                   │ ← Sketchware
15s     ├─────────────────────────────────────────┤
        │ Compile resources (AAPT2)              │
20s     ├─────────────────────────────────────────┤
        │ Compile Java code                      │
25s     ├─────────────────────────────────────────┤
        │ ProGuard obfuscation                   │
30s     ├─────────────────────────────────────────┤
        │ Create DEX files                       │
35s     ├─────────────────────────────────────────┤
        │ Build APK                              │
40s     ├─────────────────────────────────────────┤
        │ Sign APK                               │
45s     ├─────────────────────────────────────────┤
        │ ✅ COMPLETE - app-release.apk ready   │
```

---

These diagrams should help visualize:
1. The complete pipeline flow
2. How modules interact
3. Data transformations
4. Build steps in detail
5. Error recovery process
6. File organization
7. Integration points
8. Performance timeline

All diagrams reflect the **revised architecture that leverages Sketchware's ProjectBuilder**.
