package dev.tooltipstudio.config;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Local and resource-pack styles share IDs relative to styles/, without the .json suffix. */
public final class StyleFiles {
    private static final Gson GSON = new Gson();

    private StyleFiles() {}

    public static String id(String relativeJsonPath) {
        Style.require(relativeJsonPath != null && relativeJsonPath.matches("[a-z0-9_-]+(?:/[a-z0-9_-]+)*\\.json"),
                "invalid style path: " + relativeJsonPath
                        + " (use lowercase letters, digits, _ or - in each folder/file name)");
        return relativeJsonPath.substring(0, relativeJsonPath.length() - ".json".length());
    }

    public static Map<String, Style> loadLocal(Path directory, Set<String> overridden) throws IOException {
        Map<String, Style> styles = new LinkedHashMap<>();
        // Do not follow directory or file links while recursively discovering local definitions.
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)
                    && p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String relative = directory.relativize(file).toString().replace(File.separatorChar, '/');
                String name = id(relative);
                // Only the complete relative path is overridden, never another folder's same basename.
                if (overridden.contains(name)) continue;
                try (var reader = Files.newBufferedReader(file)) {
                    Style style = GSON.fromJson(reader, Style.class);
                    Style.require(style != null, "JSON cannot be null");
                    style.validate();
                    styles.put(name, style);
                } catch (IOException | RuntimeException e) {
                    throw new IllegalArgumentException("styles/" + relative + ": " + e.getMessage(), e);
                }
            }
        }
        return styles;
    }
}
