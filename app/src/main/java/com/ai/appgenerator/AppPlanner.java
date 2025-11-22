package com.ai.appgenerator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App Planner - First phase of AI-powered app generation
 * Uses Gemini's thinking mode to plan app architecture
 * 
 * Input: Natural language description (e.g., "Make me a browser")
 * Output: Structured app plan (screens, features, permissions, libraries)
 */
public class AppPlanner {
    private static final String TAG = "AppPlanner";

    private final Gson gson;

    public AppPlanner() {
        this.gson = new Gson();
    }

    /**
     * Generate an app plan from user description
     */
    public AppPlan planApp(String userDescription) {
        // This will be called with Gemini thinking enabled
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(userDescription);

        AppPlan plan = new AppPlan();
        plan.setDescription(userDescription);
        
        // Structure the response we expect from Gemini
        plan.setScreens(generateScreens(userDescription));
        plan.setFeatures(generateFeatures(userDescription));
        plan.setPermissions(generatePermissions(userDescription));
        plan.setDependencies(generateDependencies(userDescription));
        
        return plan;
    }

    /**
     * Build system prompt for planning
     */
    private String buildSystemPrompt() {
        return "You are an expert Android app architect. " +
               "When given an app description, you must:\n" +
               "1. Plan the app architecture\n" +
               "2. Define screens/activities\n" +
               "3. List required features\n" +
               "4. Identify permissions needed\n" +
               "5. Recommend libraries\n" +
               "6. Suggest UI/UX patterns\n" +
               "Always respond with valid JSON.";
    }

    /**
     * Build user prompt
     */
    private String buildUserPrompt(String userDescription) {
        return "Plan an Android app with the following description:\n" +
               userDescription + "\n" +
               "Respond with JSON containing: screens, features, permissions, dependencies";
    }

    /**
     * Generate screen/activity list from description
     */
    private List<Screen> generateScreens(String description) {
        List<Screen> screens = new ArrayList<>();

        // Parse description for keywords
        if (description.toLowerCase().contains("browser")) {
            screens.add(new Screen("MainActivity", "Main web view for browsing", true));
            screens.add(new Screen("HistoryActivity", "View browsing history", false));
            screens.add(new Screen("BookmarksActivity", "Manage bookmarks", false));
            screens.add(new Screen("SettingsActivity", "App settings", false));
        } else if (description.toLowerCase().contains("todo") || description.toLowerCase().contains("task")) {
            screens.add(new Screen("MainActivity", "Task list view", true));
            screens.add(new Screen("AddTaskActivity", "Add new task", false));
            screens.add(new Screen("TaskDetailActivity", "Task details", false));
        } else if (description.toLowerCase().contains("chat")) {
            screens.add(new Screen("MainActivity", "Chat list", true));
            screens.add(new Screen("ChatActivity", "Chat conversation", false));
            screens.add(new Screen("ProfileActivity", "User profile", false));
        } else {
            // Default structure
            screens.add(new Screen("MainActivity", "Main screen", true));
            screens.add(new Screen("SettingsActivity", "Settings", false));
        }

        return screens;
    }

    /**
     * Generate feature list from description
     */
    private List<String> generateFeatures(String description) {
        List<String> features = new ArrayList<>();

        description = description.toLowerCase();

        if (description.contains("browser")) {
            features.add("WebView with URL navigation");
            features.add("Back/Forward buttons");
            features.add("Bookmark management");
            features.add("History tracking");
            features.add("Search functionality");
        }

        if (description.contains("offline")) {
            features.add("Offline mode");
            features.add("Local storage");
        }

        if (description.contains("sync")) {
            features.add("Cloud synchronization");
            features.add("Real-time updates");
        }

        if (description.contains("notification")) {
            features.add("Push notifications");
            features.add("Notification center");
        }

        if (description.contains("dark") || description.contains("theme")) {
            features.add("Dark mode");
            features.add("Theme customization");
        }

        if (features.isEmpty()) {
            features.add("Core functionality");
            features.add("Settings management");
            features.add("User interface");
        }

        return features;
    }

    /**
     * Generate permission list from description
     */
    private List<String> generatePermissions(String description) {
        List<String> permissions = new ArrayList<>();

        description = description.toLowerCase();

        permissions.add("android.permission.INTERNET"); // Nearly all apps need this

        if (description.contains("browser") || description.contains("download")) {
            permissions.add("android.permission.WRITE_EXTERNAL_STORAGE");
            permissions.add("android.permission.READ_EXTERNAL_STORAGE");
        }

        if (description.contains("camera")) {
            permissions.add("android.permission.CAMERA");
        }

        if (description.contains("location")) {
            permissions.add("android.permission.ACCESS_FINE_LOCATION");
            permissions.add("android.permission.ACCESS_COARSE_LOCATION");
        }

        if (description.contains("contact")) {
            permissions.add("android.permission.READ_CONTACTS");
            permissions.add("android.permission.WRITE_CONTACTS");
        }

        if (description.contains("notification")) {
            permissions.add("android.permission.POST_NOTIFICATIONS");
        }

        if (description.contains("bluetooth")) {
            permissions.add("android.permission.BLUETOOTH");
            permissions.add("android.permission.BLUETOOTH_ADMIN");
        }

        if (description.contains("microphone") || description.contains("voice")) {
            permissions.add("android.permission.RECORD_AUDIO");
        }

        return permissions;
    }

    /**
     * Generate dependencies from description
     */
    private List<Dependency> generateDependencies(String description) {
        List<Dependency> dependencies = new ArrayList<>();

        description = description.toLowerCase();

        // Core libraries (always included)
        dependencies.add(new Dependency("androidx.appcompat:appcompat", "Latest"));
        dependencies.add(new Dependency("com.google.android.material:material", "Latest"));

        if (description.contains("network") || description.contains("api")) {
            dependencies.add(new Dependency("com.squareup.okhttp3:okhttp", "4.x"));
            dependencies.add(new Dependency("com.google.code.gson:gson", "Latest"));
        }

        if (description.contains("database") || description.contains("storage")) {
            dependencies.add(new Dependency("androidx.room:room-runtime", "Latest"));
        }

        if (description.contains("image")) {
            dependencies.add(new Dependency("com.github.bumptech.glide:glide", "Latest"));
        }

        if (description.contains("firebase")) {
            dependencies.add(new Dependency("com.google.firebase:firebase-analytics", "Latest"));
            dependencies.add(new Dependency("com.google.firebase:firebase-messaging", "Latest"));
        }

        if (description.contains("animation")) {
            dependencies.add(new Dependency("com.airbnb.android:lottie", "Latest"));
        }

        return dependencies;
    }

    /**
     * App Plan data class
     */
    public static class AppPlan {
        private String description;
        private List<Screen> screens;
        private List<String> features;
        private List<String> permissions;
        private List<Dependency> dependencies;
        private Map<String, Object> metadata;

        public AppPlan() {
            this.metadata = new HashMap<>();
            this.metadata.put("createdAt", System.currentTimeMillis());
            this.metadata.put("version", "1.0");
        }

        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<Screen> getScreens() { return screens; }
        public void setScreens(List<Screen> screens) { this.screens = screens; }

        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }

        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }

        public List<Dependency> getDependencies() { return dependencies; }
        public void setDependencies(List<Dependency> dependencies) { this.dependencies = dependencies; }

        public Map<String, Object> getMetadata() { return metadata; }

        public String toJson() {
            return new Gson().toJson(this);
        }
    }

    /**
     * Screen/Activity definition
     */
    public static class Screen {
        private String name;
        private String description;
        private boolean isLauncher;

        public Screen(String name, String description, boolean isLauncher) {
            this.name = name;
            this.description = description;
            this.isLauncher = isLauncher;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isLauncher() { return isLauncher; }
    }

    /**
     * Dependency definition
     */
    public static class Dependency {
        private String name;
        private String version;

        public Dependency(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() { return name; }
        public String getVersion() { return version; }
    }
}
