# 🚀 GitHub Push & Actions Setup Guide

## Step 1: Create GitHub Repository

If you haven't already, create a new repository on GitHub:

1. Go to **https://github.com/new**
2. Repository name: `sketchware-ai` (or your preferred name)
3. Description: "Sketchware Pro with Gemini 2.5 Pro AI Integration"
4. Make it **Public** (so Actions can work)
5. **Do NOT** initialize with README (we have one)
6. Click **Create repository**

---

## Step 2: Add Remote & Push (Copy & Run These Commands)

Replace `YOUR_USERNAME` with your GitHub username and run:

```bash
cd "d:\Datasets of Junk\New folder\Sketchware-Pro-main"

git remote add origin https://github.com/YOUR_USERNAME/sketchware-ai.git

git branch -M main

git push -u origin main
```

---

## Step 3: GitHub Actions Secrets Setup

The build workflow needs API keys. Add them to GitHub:

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

**Add these secrets:**

### Secret 1: GEMINI_PRIMARY_API_KEY
- Name: `GEMINI_PRIMARY_API_KEY`
- Value: Your Gemini API key starting with `AIza...`

### Secret 2: GEMINI_BACKUP_API_KEY (Optional)
- Name: `GEMINI_BACKUP_API_KEY`
- Value: Backup API key (same format)

### Secret 3: SKETCHUB_API_KEY (Optional)
- Name: `SKETCHUB_API_KEY`
- Value: If you have Sketchub integration

---

## Step 4: Verify GitHub Actions

1. Go to your repository on GitHub
2. Click **Actions** tab
3. You should see the workflow: **"Build APKs"** and **"AI-Powered APK Generation"**
4. Both workflows are ready to trigger

---

## Step 5: Test the Build

### Option A: Trigger Build via GitHub Actions UI

1. Go to **Actions** tab
2. Click **"AI-Powered APK Generation"** workflow
3. Click **"Run workflow"** button
4. Enter:
   - App description: `"Make me a calculator app"`
   - Project name: `"Calculator"`
5. Click **"Run workflow"**
6. Watch the build progress in real-time

### Option B: Trigger via Repository Dispatch (API)

```bash
curl -X POST \
  https://api.github.com/repos/YOUR_USERNAME/sketchware-ai/dispatches \
  -H "Authorization: token YOUR_GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+raw+json" \
  -d '{
    "event_type": "ai-app-build-request",
    "client_payload": {
      "app_description": "Make me a calculator app",
      "project_name": "Calculator"
    }
  }'
```

---

## Workflows Overview

### Workflow 1: Regular Build (`android.yml`)
- **Trigger:** Every push to `main`
- **Action:** Build standard Sketchware APK
- **Output:** APK artifact in Actions

### Workflow 2: AI Generation (`ai-build.yml`)
- **Trigger:** Manual or repository dispatch
- **Action:** Generate app with Gemini AI
- **Output:** AI-generated APK artifact

---

## File Structure in GitHub

```
your-repo/
├── .github/workflows/
│   ├── android.yml ─────────────── Regular build
│   └── ai-build.yml ───────────── AI generation
├── app/
│   └── src/main/java/com/ai/
│       ├── gemini/ ────────────── Gemini API
│       ├── generation/ ────────── Code generation
│       ├── integration/ ────────── Sketchware bridge (NEW)
│       ├── errorrecovery/ ─────── Error handling
│       └── ui/ ────────────────── UI components
├── build.gradle
├── settings.gradle
├── README.md
├── 00_START_HERE.md
├── KEY_INSIGHT.md
├── REVISED_ARCHITECTURE.md
└── [all other docs]
```

---

## Troubleshooting

### Build Fails with "API key not found"
**Solution:**
1. Go to **Settings** → **Secrets**
2. Verify `GEMINI_PRIMARY_API_KEY` is set
3. Ensure key starts with `AIza`
4. Rerun the workflow

### Build Fails with "Gradle not found"
**Solution:**
- Verify `gradlew` file exists in root
- Check `.github/workflows/ai-build.yml` has JDK 17 setup
- Rerun the workflow

### APK not in artifacts
**Solution:**
1. Check the build logs in Actions
2. Look for error messages
3. Verify Android SDK is installed
4. Check Gradle build output

---

## Monitoring Builds

### Real-Time View
1. Go to **Actions** tab
2. Click the running workflow
3. Click **Build APK** or **Generate App with AI**
4. Watch logs in real-time

### Build History
1. Go to **Actions** tab
2. See all past runs
3. Click any to see details
4. Download artifacts if successful

### Notifications
- GitHub sends email on workflow failure
- You can customize notifications in Settings

---

## Next Steps

1. **Create GitHub repo** (if not done)
2. **Add remote & push code** (following Step 2)
3. **Add secrets** (following Step 3)
4. **Test AI workflow** (following Step 5)
5. **Monitor build** (following Monitoring section)

---

## Quick Command Reference

```bash
# Navigate to project
cd "d:\Datasets of Junk\New folder\Sketchware-Pro-main"

# Check git status
git status

# Check remote
git remote -v

# View git log
git log --oneline

# Push latest commits
git push origin main

# Create new branch
git checkout -b feature/my-feature

# Switch to main
git checkout main

# Pull latest
git pull origin main
```

---

## Security Notes

### API Keys
✅ **Never commit API keys**  
✅ **Always use GitHub Secrets**  
✅ **Rotate keys regularly**  
✅ **Use different keys for dev/prod**  

### Repository
✅ **Make public** (for free Actions minutes)  
✅ **Add `.gitignore`** (already included)  
✅ **Review Collaborators** (Settings → Collaborators)  

---

## Resources

- **GitHub Actions Docs:** https://docs.github.com/actions
- **Gemini API Docs:** https://ai.google.dev
- **Sketchware Documentation:** See README.md
- **Android Gradle Plugin:** https://developer.android.com/studio/releases/gradle-plugin

---

## Support

If you encounter issues:

1. **Check workflow logs** in Actions tab
2. **Review error messages** carefully
3. **Verify secrets are set** in Settings
4. **Check file permissions** (all should be readable)
5. **Ensure JDK 17** is specified

---

**Status:** Ready to push! 🚀

**Next:** Follow the step-by-step guide above to:
1. Create GitHub repo
2. Push code
3. Add secrets
4. Test build

Let me know your GitHub username if you need help!
