package com.ai.appgenerator;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 * Code Generator - Generates Android/Java code from app plan
 * Converts AI-generated code into compilable Android code
 * Creates activities, services, adapters, and helper classes
 */
public class CodeGenerator {
    private static final String TAG = "CodeGenerator";
    private final Gson gson;

    public CodeGenerator() {
        this.gson = new Gson();
    }

    /**
     * Generate code for a screen/activity
     */
    public GeneratedCode generateActivityCode(String activityName, String description, AppPlanner.AppPlan plan) {
        GeneratedCode code = new GeneratedCode();
        code.setClassName(activityName);
        code.setPackageName("pro.sketchware.generated");

        // Generate Activity Java code
        String activityJava = generateActivityJava(activityName, description, plan);
        code.addFile("activities/" + activityName + ".java", activityJava);

        // Generate corresponding layout XML
        String layoutXml = generateLayoutXml(activityName, description);
        code.addFile("layouts/" + activityName.toLowerCase() + ".xml", layoutXml);

        return code;
    }

    /**
     * Generate Activity Java code
     */
    private String generateActivityJava(String activityName, String description, AppPlanner.AppPlan plan) {
        StringBuilder sb = new StringBuilder();

        sb.append("package pro.sketchware.generated.activities;\n\n");
        sb.append("import android.os.Bundle;\n");
        sb.append("import androidx.appcompat.app.AppCompatActivity;\n");
        sb.append("import android.view.View;\n");
        sb.append("import android.widget.*;\n\n");

        sb.append("public class ").append(activityName).append(" extends AppCompatActivity {\n\n");

        // Declare UI components
        sb.append("    private TextView titleView;\n");
        sb.append("    private LinearLayout contentLayout;\n");
        sb.append("    private ProgressBar progressBar;\n\n");

        // onCreate method
        sb.append("    @Override\n");
        sb.append("    protected void onCreate(Bundle savedInstanceState) {\n");
        sb.append("        super.onCreate(savedInstanceState);\n");
        sb.append("        setContentView(R.layout.").append(activityName.toLowerCase()).append(");\n");
        sb.append("        initializeViews();\n");
        sb.append("        setupListeners();\n");
        sb.append("        loadContent();\n");
        sb.append("    }\n\n");

        // Initialize views
        sb.append("    private void initializeViews() {\n");
        sb.append("        titleView = findViewById(R.id.title_view);\n");
        sb.append("        contentLayout = findViewById(R.id.content_layout);\n");
        sb.append("        progressBar = findViewById(R.id.progress_bar);\n");
        sb.append("    }\n\n");

        // Setup listeners
        sb.append("    private void setupListeners() {\n");
        sb.append("        // TODO: Add click listeners and event handlers\n");
        sb.append("    }\n\n");

        // Load content
        sb.append("    private void loadContent() {\n");
        sb.append("        // TODO: Load and display content\n");
        sb.append("        titleView.setText(\"").append(description).append("\");\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generate layout XML for activity
     */
    private String generateLayoutXml(String activityName, String description) {
        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<LinearLayout\n");
        sb.append("    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
        sb.append("    android:layout_width=\"match_parent\"\n");
        sb.append("    android:layout_height=\"match_parent\"\n");
        sb.append("    android:orientation=\"vertical\"\n");
        sb.append("    android:padding=\"16dp\">\n\n");

        // Title
        sb.append("    <TextView\n");
        sb.append("        android:id=\"@+id/title_view\"\n");
        sb.append("        android:layout_width=\"match_parent\"\n");
        sb.append("        android:layout_height=\"wrap_content\"\n");
        sb.append("        android:text=\"").append(description).append("\"\n");
        sb.append("        android:textSize=\"24sp\"\n");
        sb.append("        android:textStyle=\"bold\"\n");
        sb.append("        android:layout_marginBottom=\"16dp\" />\n\n");

        // Content container
        sb.append("    <LinearLayout\n");
        sb.append("        android:id=\"@+id/content_layout\"\n");
        sb.append("        android:layout_width=\"match_parent\"\n");
        sb.append("        android:layout_height=\"0dp\"\n");
        sb.append("        android:layout_weight=\"1\"\n");
        sb.append("        android:orientation=\"vertical\" />\n\n");

        // Progress bar
        sb.append("    <ProgressBar\n");
        sb.append("        android:id=\"@+id/progress_bar\"\n");
        sb.append("        android:layout_width=\"match_parent\"\n");
        sb.append("        android:layout_height=\"wrap_content\"\n");
        sb.append("        android:visibility=\"gone\" />\n\n");

        sb.append("</LinearLayout>\n");

        return sb.toString();
    }

    /**
     * Generate helper/utility class
     */
    public String generateHelperCode(String helperName, String purpose) {
        StringBuilder sb = new StringBuilder();

        sb.append("package pro.sketchware.generated.utils;\n\n");
        sb.append("import android.content.Context;\n\n");
        sb.append("public class ").append(helperName).append(" {\n\n");
        sb.append("    private Context context;\n\n");
        sb.append("    public ").append(helperName).append("(Context context) {\n");
        sb.append("        this.context = context;\n");
        sb.append("    }\n\n");
        sb.append("    // TODO: Implement ").append(purpose).append("\n\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generated code container
     */
    public static class GeneratedCode {
        private String className;
        private String packageName;
        private Map<String, String> files;

        public GeneratedCode() {
            this.files = new HashMap<>();
        }

        public void addFile(String path, String content) {
            files.put(path, content);
        }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public Map<String, String> getFiles() { return files; }

        public int getFileCount() { return files.size(); }
    }
}
