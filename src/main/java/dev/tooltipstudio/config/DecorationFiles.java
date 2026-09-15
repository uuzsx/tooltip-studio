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

public final class DecorationFiles {
    private static final Gson GSON = new Gson();
    private DecorationFiles() {}

    public static Map<String, DecorationDefinition> loadLocal(Path directory, Set<String> overridden) throws IOException {
        Map<String, DecorationDefinition> definitions = new LinkedHashMap<>();
        if (!Files.exists(directory)) return definitions;
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)
                    && p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String relative = directory.relativize(file).toString().replace(File.separatorChar, '/');
                try {
                    String id = StyleFiles.id(relative);
                    if (overridden.contains(id)) continue;
                    try (var reader = Files.newBufferedReader(file)) {
                        DecorationDefinition definition = GSON.fromJson(reader, DecorationDefinition.class);
                        Style.require(definition != null, "decoration JSON cannot be null");
                        definition.validate();
                        definitions.put(id, definition);
                    }
                } catch (IOException | RuntimeException e) {
                    throw new IllegalArgumentException("decorations/" + relative + ": " + e.getMessage(), e);
                }
            }
        }
        return definitions;
    }
}
