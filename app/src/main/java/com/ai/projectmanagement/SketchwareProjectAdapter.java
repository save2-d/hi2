package com.ai.projectmanagement;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Sketchware Project Adapter
 * Handles reading/writing Sketchware project format (.sketchware files)
 * Converts between Sketchware JSON and generated code
 */
public class SketchwareProjectAdapter {
    private static final String TAG = "SketchwareProjectAdapter";
    private static final String SKETCHWARE_DIR = "/sketchware";
    private static final String PROJECT_EXTENSION = ".sketchware";
    
    private final Context context;
    private final Gson gson;

    public SketchwareProjectAdapter(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    /**
     * Create a new Sketchware project from AI-generated content
     */
    public File createProjectFromGenerated(String projectName, ProjectContent content) throws IOException {
        // Create project directory
        File projectDir = getProjectDirectory(projectName);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        // Create src.json (activities and code)
        File srcFile = new File(projectDir, "src.json");
        srcFile.createNewFile();
        writeSrcJson(srcFile, content);

        // Create res.json (resources)
        File resFile = new File(projectDir, "res.json");
        resFile.createNewFile();
        writeResJson(resFile, content);

        // Create lib.json (library dependencies)
        File libFile = new File(projectDir, "lib.json");
        libFile.createNewFile();
        writeLibJson(libFile, content);

        // Create metadata
        File metaFile = new File(projectDir, "metadata.json");
        metaFile.createNewFile();
        writeMetadata(metaFile, projectName, content);

        return projectDir;
    }

    /**
     * Read existing Sketchware project
     */
    public ProjectContent readProject(String projectName) throws IOException {
        File projectDir = getProjectDirectory(projectName);
        
        if (!projectDir.exists()) {
            throw new IOException("Project directory not found: " + projectDir.getAbsolutePath());
        }

        ProjectContent content = new ProjectContent();
        
        // Read src.json
        File srcFile = new File(projectDir, "src.json");
        if (srcFile.exists()) {
            content.setSourceCode(readJsonFile(srcFile));
        }

        // Read res.json
        File resFile = new File(projectDir, "res.json");
        if (resFile.exists()) {
            content.setResources(readJsonFile(resFile));
        }

        // Read lib.json
        File libFile = new File(projectDir, "lib.json");
        if (libFile.exists()) {
            content.setLibraries(readJsonFile(libFile));
        }

        return content;
    }

    /**
     * Export project as .sketchware ZIP file
     */
    public File exportProjectAsZip(String projectName, File outputDir) throws IOException {
        File projectDir = getProjectDirectory(projectName);
        File zipFile = new File(outputDir, projectName + PROJECT_EXTENSION);

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

        // Add all files from project directory to ZIP
        addFilesToZip(zos, projectDir, "");

        zos.close();
        return zipFile;
    }

    /**
     * Import project from ZIP file
     */
    public String importProjectFromZip(File zipFile) throws IOException {
        String projectName = zipFile.getName().replace(PROJECT_EXTENSION, "");
        File projectDir = getProjectDirectory(projectName);
        
        if (projectDir.exists()) {
            projectName = projectName + "_" + System.currentTimeMillis();
            projectDir = getProjectDirectory(projectName);
        }

        projectDir.mkdirs();

        // Extract ZIP contents
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
            File file = new File(projectDir, entry.getName());
            
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                file.createNewFile();
                
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                copyStream(zis, bos);
                bos.close();
            }
        }

        zis.close();
        return projectName;
    }

    /**
     * Get Sketchware projects directory
     */
    public File getSketchwareDirectory() {
        File externalStorage = context.getExternalFilesDir(null);
        return new File(externalStorage.getParent().getParent(), SKETCHWARE_DIR);
    }

    /**
     * Get specific project directory
     */
    private File getProjectDirectory(String projectName) {
        return new File(getSketchwareDirectory(), "projects/" + projectName);
    }

    /**
     * Write src.json (activities/code)
     */
    private void writeSrcJson(File file, ProjectContent content) throws IOException {
        JsonObject srcJson = new JsonObject();
        srcJson.add("activities", gson.toJsonTree(content.getActivities()));
        srcJson.add("services", gson.toJsonTree(content.getServices()));
        srcJson.add("broadcast_receivers", gson.toJsonTree(content.getBroadcastReceivers()));
        srcJson.add("content_providers", gson.toJsonTree(content.getContentProviders()));

        writeJsonToFile(file, srcJson);
    }

    /**
     * Write res.json (layouts, strings, colors, etc)
     */
    private void writeResJson(File file, ProjectContent content) throws IOException {
        JsonObject resJson = new JsonObject();
        resJson.add("layouts", gson.toJsonTree(content.getLayouts()));
        resJson.add("strings", gson.toJsonTree(content.getStrings()));
        resJson.add("colors", gson.toJsonTree(content.getColors()));
        resJson.add("drawables", gson.toJsonTree(content.getDrawables()));

        writeJsonToFile(file, resJson);
    }

    /**
     * Write lib.json (dependencies)
     */
    private void writeLibJson(File file, ProjectContent content) throws IOException {
        JsonObject libJson = new JsonObject();
        libJson.add("dependencies", gson.toJsonTree(content.getDependencies()));
        libJson.add("permissions", gson.toJsonTree(content.getPermissions()));

        writeJsonToFile(file, libJson);
    }

    /**
     * Write metadata.json
     */
    private void writeMetadata(File file, String projectName, ProjectContent content) throws IOException {
        JsonObject meta = new JsonObject();
        meta.addProperty("name", projectName);
        meta.addProperty("created_by", "AI");
        meta.addProperty("created_at", System.currentTimeMillis());
        meta.addProperty("version", "1.0");
        meta.addProperty("target_sdk", 28);
        meta.addProperty("min_sdk", 26);

        writeJsonToFile(file, meta);
    }

    /**
     * Helper: Write JSON to file
     */
    private void writeJsonToFile(File file, JsonObject json) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(gson.toJson(json).getBytes("UTF-8"));
        fos.close();
    }

    /**
     * Helper: Read JSON file
     */
    private JsonObject readJsonFile(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(data);
        fis.close();
        
        String json = new String(data, "UTF-8");
        return gson.fromJson(json, JsonObject.class);
    }

    /**
     * Helper: Add files to ZIP
     */
    private void addFilesToZip(ZipOutputStream zos, File dir, String parentPath) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String entryName = parentPath + file.getName();
            
            if (file.isDirectory()) {
                entryName += "/";
                zos.putNextEntry(new ZipEntry(entryName));
                addFilesToZip(zos, file, entryName);
            } else {
                zos.putNextEntry(new ZipEntry(entryName));
                FileInputStream fis = new FileInputStream(file);
                copyStream(fis, zos);
                fis.close();
            }
        }
    }

    /**
     * Helper: Copy stream
     */
    private void copyStream(java.io.InputStream in, java.io.OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Project content data class
     */
    public static class ProjectContent {
        private java.util.List<Object> activities;
        private java.util.List<Object> services;
        private java.util.List<Object> broadcastReceivers;
        private java.util.List<Object> contentProviders;
        private java.util.List<Object> layouts;
        private java.util.Map<String, String> strings;
        private java.util.Map<String, String> colors;
        private java.util.Map<String, Object> drawables;
        private java.util.List<String> dependencies;
        private java.util.List<String> permissions;
        private JsonObject sourceCode;
        private JsonObject resources;
        private JsonObject libraries;

        public ProjectContent() {
            this.activities = new java.util.ArrayList<>();
            this.services = new java.util.ArrayList<>();
            this.broadcastReceivers = new java.util.ArrayList<>();
            this.contentProviders = new java.util.ArrayList<>();
            this.layouts = new java.util.ArrayList<>();
            this.strings = new java.util.HashMap<>();
            this.colors = new java.util.HashMap<>();
            this.drawables = new java.util.HashMap<>();
            this.dependencies = new java.util.ArrayList<>();
            this.permissions = new java.util.ArrayList<>();
        }

        // Getters and setters
        public java.util.List<Object> getActivities() { return activities; }
        public void setActivities(java.util.List<Object> activities) { this.activities = activities; }

        public java.util.List<Object> getServices() { return services; }
        public void setServices(java.util.List<Object> services) { this.services = services; }

        public java.util.List<Object> getBroadcastReceivers() { return broadcastReceivers; }
        public void setBroadcastReceivers(java.util.List<Object> broadcastReceivers) { this.broadcastReceivers = broadcastReceivers; }

        public java.util.List<Object> getContentProviders() { return contentProviders; }
        public void setContentProviders(java.util.List<Object> contentProviders) { this.contentProviders = contentProviders; }

        public java.util.List<Object> getLayouts() { return layouts; }
        public void setLayouts(java.util.List<Object> layouts) { this.layouts = layouts; }

        public java.util.Map<String, String> getStrings() { return strings; }
        public void setStrings(java.util.Map<String, String> strings) { this.strings = strings; }

        public java.util.Map<String, String> getColors() { return colors; }
        public void setColors(java.util.Map<String, String> colors) { this.colors = colors; }

        public java.util.Map<String, Object> getDrawables() { return drawables; }
        public void setDrawables(java.util.Map<String, Object> drawables) { this.drawables = drawables; }

        public java.util.List<String> getDependencies() { return dependencies; }
        public void setDependencies(java.util.List<String> dependencies) { this.dependencies = dependencies; }

        public java.util.List<String> getPermissions() { return permissions; }
        public void setPermissions(java.util.List<String> permissions) { this.permissions = permissions; }

        public JsonObject getSourceCode() { return sourceCode; }
        public void setSourceCode(JsonObject sourceCode) { this.sourceCode = sourceCode; }

        public JsonObject getResources() { return resources; }
        public void setResources(JsonObject resources) { this.resources = resources; }

        public JsonObject getLibraries() { return libraries; }
        public void setLibraries(JsonObject libraries) { this.libraries = libraries; }
    }
}
