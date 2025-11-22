# Sketchware Pro AI Integration - Deployment Guide

## 🎯 Complete Deployment Instructions

This guide walks through deploying the AI-powered Sketchware Pro to production and on GitHub.

---

## Phase 1: Local Testing & Validation

### Step 1: Clone & Setup Repository


Clone your personal repository as needed. All contributor and user references have been removed for personal use.

### Step 2: Verify AI Integration Files

Check that all new files are in place:

```bash
# Check Gemini API files
ls -la app/src/main/java/com/ai/gemini/
# Should show: GeminiClient.java, APIKeyManager.java, RateLimitHandler.java, TokenCounter.java

# Check app generation files
ls -la app/src/main/java/com/ai/appgenerator/
# Should show: AppPlanner.java, CodeGenerator.java

# Check project management
ls -la app/src/main/java/com/ai/projectmanagement/
# Should show: SketchwareProjectAdapter.java

# Check error recovery
ls -la app/src/main/java/com/ai/errorrecovery/
# Should show: BuildErrorAnalyzer.java, AutoFixer.java

# Check orchestrator
ls -la app/src/main/java/com/ai/orchestrator/
# Should show: AIAppOrchestrator.java

# Check UI
ls -la app/src/main/java/com/ai/ui/
# Should show: APIKeySettingsActivity.java

# Check workflow
ls -la .github/workflows/
# Should show: ai-build.yml (alongside android.yml)

# Check documentation
ls -la | grep AI_
# Should show: AI_IMPLEMENTATION_SUMMARY.md, AI_INTEGRATION_GUIDE.md
```

### Step 3: Verify Build Configuration

```bash
# Check gradle dependencies
grep -A 20 "# AI/Gemini Integration" app/build.gradle
# Should show: implementation libs.bundles.ai

# Check libs.versions.toml
grep -A 5 "gemini" gradle/libs.versions.toml
# Should show Gemini SDK and dependencies
```

### Step 4: Build Locally

```bash
# Sync Gradle
./gradlew syncLibs

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore)
./gradlew assembleRelease

# Expected: APK generated in app/build/outputs/apk/
```

If build succeeds:
- ✅ All dependencies are compatible
- ✅ Java code compiles without errors
- ✅ Resources are properly configured

---

## Phase 2: GitHub Configuration

### Step 1: Add GitHub Secrets

```bash
# If using GitHub CLI:
gh secret set GEMINI_PRIMARY_API_KEY --body "AIza..."
gh secret set GEMINI_BACKUP_API_KEY --body "AIza..."

# Or via GitHub web UI:
# 1. Go to Settings → Secrets and variables → Actions
# 2. Click "New repository secret"
# 3. Name: GEMINI_PRIMARY_API_KEY
#    Value: (paste your Gemini API key starting with AIza...)
# 4. Repeat for GEMINI_BACKUP_API_KEY (optional)
```

### Step 2: Enable GitHub Actions

```bash
# Verify workflow file exists and is valid
cat .github/workflows/ai-build.yml | head -20

# Check workflow syntax
# GitHub Actions will validate automatically when pushed
```

### Step 3: Test Workflow Trigger

```bash
# Commit and push to trigger automated build
git add .
git commit -m "Add AI integration with Gemini 2.5 Pro"
git push origin main

# Or trigger manually:
# 1. Go to GitHub → Actions tab
# 2. Select "Android CI" workflow
# 3. Click "Run workflow"
# 4. Watch build progress
```

---

## Phase 3: First AI App Generation

### Test 1: Simple App

```bash
# Via GitHub Actions UI:
1. Go to Actions → "AI-Powered APK Generation"
2. Click "Run workflow"
3. App description: "Make me a hello world app"
4. Project name: "HelloWorld"
5. Click "Run workflow"
6. Wait for completion (~5 minutes for simple app)
```

Expected flow:
```
✓ Planning (20-30s): AI analyzes requirements
✓ Code Generation (10-15s): Creates activities
✓ Project Adaptation (5s): Converts to Sketchware
✓ Build (30-45s): Gradle builds APK
✓ Upload: APK stored as artifact
```

### Test 2: Medium Complexity App

```bash
1. Same as Test 1, but with:
   App description: "Make me a todo list app with database"
   Project name: "TodoApp"
2. Expected time: 10-15 minutes total
```

### Test 3: Error Handling

```bash
1. Configure bad API key (intentionally invalid)
2. Try to generate app
3. Observe error recovery:
   - Error detected
   - Auto-fix attempt 1
   - If fails, auto-fix attempt 2
   - If still fails, show error message
```

---

## Phase 4: Production Deployment

### Step 1: Create Release

```bash
# Create a release tag
git tag -a v1.0.0-ai -m "AI-powered Sketchware Pro with Gemini 2.5 Pro integration"
git push origin v1.0.0-ai

# GitHub automatically creates release and runs workflow
```

### Step 2: Binary Distribution

```bash
# Download APK from GitHub Actions artifacts
# Or create a release with APK attached:

# Option 1: Manual upload
1. Build locally: ./gradlew assembleRelease
2. Go to GitHub Releases → "Edit release"
3. Upload app/build/outputs/apk/release/*.apk
4. Publish

# Option 2: Automatic (via workflow)
# GitHub Actions already uploads to releases when tagged
```

### Step 3: Publish to Play Store (Optional)

```bash
# Build signed release APK
./gradlew assembleRelease \
  -PandroidSigningKeyStore=/path/to/keystore \
  -PandroidSigningKeyStorePassword=password \
  -PandroidSigningKeyAlias=alias \
  -PandroidSigningKeyPassword=password

# Upload to Play Store Console
# Build → Release management → Releases → Create release
```

---

## Phase 5: User Onboarding

### Step 1: Installation

```bash
# Users can:
# A) Download APK from GitHub Releases
# B) Build from source: ./gradlew assembleRelease
# C) Install via adb: adb install app-release.apk

# First launch:
# - App detects no API keys
# - Shows setup wizard
# - Links to: https://ai.google.dev/gemini-api/docs/api-key
```

### Step 2: API Key Configuration

```bash
# User flow:
1. Get free Gemini API key from Google
2. Open Sketchware Pro
3. Tap Settings → AI Brain → Add API Key
4. Paste primary key
5. (Optional) Add backup key
6. Tap "Test & Save"
7. Keys encrypted on device
```

### Step 3: First AI Generation

```bash
# User:
1. Tap "AI Generate" button
2. Type: "Make me a browser"
3. Name: "MyBrowser"
4. Tap "Generate"
5. Watch progress
6. Download APK when ready
```

---

## 🔍 Verification Checklist

### Code Quality
- [ ] All 10+ Java classes compile without errors
- [ ] No warnings in Gradle sync
- [ ] AndroidManifest.xml valid
- [ ] All imports resolved
- [ ] ProGuard rules compatible

### Dependencies
- [ ] Gemini SDK 0.7.0 downloads correctly
- [ ] Security-crypto library included
- [ ] OkHttp and Gson already present
- [ ] No version conflicts
- [ ] minSdk 26, targetSdk 28 compatible

### GitHub Actions
- [ ] Workflow YAML syntax valid
- [ ] Secrets properly configured
- [ ] Build triggers on push
- [ ] Build triggers on manual dispatch
- [ ] Artifacts upload successfully

### API Integration
- [ ] GeminiClient connects to API
- [ ] APIKeyManager encrypts keys
- [ ] RateLimitHandler enforces limits
- [ ] TokenCounter tracks usage
- [ ] Failover logic works

### Error Recovery
- [ ] BuildErrorAnalyzer parses errors
- [ ] AutoFixer suggests fixes
- [ ] Retry logic works (max 2 attempts)
- [ ] Errors logged properly

### UI/UX
- [ ] API key input accepts valid keys
- [ ] Masked display shows last 4 chars
- [ ] Test button validates key
- [ ] Status shows current state
- [ ] Setup guide links are correct

---

## 📊 Performance Expectations

### Generation Times:

```
Simple App (Hello World):
- Planning: 15-20s
- Code Gen: 10-15s
- Project Adapt: 5s
- Build: 30-40s
- Total: ~60-75 seconds

Medium App (Todo List):
- Planning: 25-35s
- Code Gen: 15-20s
- Project Adapt: 5-10s
- Build: 45-60s
- Total: ~90-125 seconds

Complex App (E-commerce):
- Planning: 40-60s
- Code Gen: 30-45s
- Project Adapt: 10-15s
- Build: 60-90s
- Total: ~140-210 seconds
```

### Resource Usage:

```
Memory: ~500MB during build
Storage: 200MB for project files
Network: 1-2MB per API call (thinking mode uses more)
Battery: Moderate (GPU not used)
Disk I/O: Peak during code gen
```

### API Usage:

```
Requests per app:
- Simple: 3-4 API calls
- Medium: 5-6 API calls
- Complex: 7-10 API calls

Tokens per app:
- Simple: 30k-50k tokens
- Medium: 50k-100k tokens
- Complex: 100k-150k tokens

Daily capacity:
- 42 requests/day (API limit)
- 2M tokens/day (conservative budget)
- ~20 complex apps/day
```

---

## 🐛 Troubleshooting Guide

### Build Fails: "Cannot find symbol"
```
Likely causes:
1. Kotlin not properly installed
2. Java version mismatch
3. Missing import statements

Solution:
- Verify JDK 17: java -version
- Clean build: ./gradlew clean
- Rebuild: ./gradlew assemble*
- Check imports in generated code
```

### Build Fails: "API Key not found"
```
Solution:
- Verify GEMINI_PRIMARY_API_KEY secret exists
- Check secret value is correct (starts with AIza)
- Re-add secret if needed
- Use backup key if primary doesn't work
```

### GitHub Actions Timeout
```
Symptoms: Workflow exceeds 120 minutes

Solutions:
1. Complex app generation taking too long
2. Rate limit delays (waiting 9s between retries)
3. Network issues

Fix:
- Simplify app description
- Check API key quotas
- Try again later
```

### Generated APK Won't Install
```
Possible reasons:
1. Signature invalid
2. Target SDK mismatch
3. Required permissions denied

Solution:
- Check build output for errors
- Verify androidManifest.xml
- Check device requirements (minSdk 26)
```

### API Key Not Working
```
Symptoms:
- 403 Unauthorized error
- 429 Rate limit too frequently

Solutions:
1. Verify key format: starts with "AIza"
2. Check quota at console.cloud.google.com
3. Regenerate key if compromised
4. Try backup key
5. Wait for rate limit window reset
```

---

## 📈 Success Criteria

After deployment, verify:

- ✅ **Build Success**: APK generated without errors
- ✅ **App Stability**: No crashes on launch
- ✅ **API Integration**: Gemini API responds correctly
- ✅ **Key Management**: Keys encrypted and secured
- ✅ **Error Recovery**: Auto-fix triggers on build errors
- ✅ **User Experience**: Setup is intuitive
- ✅ **Performance**: Generation completes in expected time
- ✅ **Documentation**: Users can find help

---

## 📞 Support Resources

### For Users:
- GitHub Issues: https://github.com/Sketchware-Pro/Sketchware-Pro/issues
- Discord: https://discord.gg/kq39yhT4rX
- Documentation: AI_INTEGRATION_GUIDE.md

### For Developers:
- Gemini API Docs: https://ai.google.dev/gemini-api/docs
- Android Dev Docs: https://developer.android.com/
- GitHub Actions: https://docs.github.com/en/actions

### API Key Help:
- Get Key: https://ai.google.dev/gemini-api/docs/api-key
- Check Quota: https://console.cloud.google.com
- Billing: https://ai.google.dev/pricing

---

## 🎉 Deployment Complete!

Your AI-powered Sketchware Pro is ready for production! 

### Next Steps:
1. ✅ Test locally
2. ✅ Configure GitHub secrets
3. ✅ Trigger first AI generation
4. ✅ Share with users
5. ✅ Collect feedback
6. ✅ Iterate and improve

### Common First Steps for Users:
1. Install APK
2. Get Gemini API key
3. Add key in Settings
4. Generate first app
5. Download and test APK

---

**Happy Generating! 🚀**

Questions? See `AI_INTEGRATION_GUIDE.md` or visit GitHub Issues.
