package dev.tooltipstudio.config;

import com.google.gson.Gson;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Definitions from the currently enabled resource packs; no files are copied into the user's config. */
public record PackDefinitions(Map<String, Style> styles, List<RuleFile> ruleFiles) {
    private static final Gson GSON = new Gson();
    public record RuleFile(String source, List<Settings.Rule> rules) {}
    public record SourcedRule(String source, Settings.Rule rule) {}
    private record RuleDocument(int schemaVersion, List<Settings.Rule> rules) {}

    public static PackDefinitions load(ResourceManager resources) throws IOException {
        Map<String, Style> styles = new LinkedHashMap<>();
        for (var entry : files(resources, "styles")) {
            String source = source(entry);
            try {
                String name = StyleFiles.id(entry.getKey().getPath().substring("styles/".length()));
                Style style = read(entry.getValue(), Style.class);
                style.validate();
                Style.require(!style.texture().startsWith("local:"), "resource-pack styles must use a resource texture ID, not local:");
                styles.put(name, style);
            } catch (IOException | RuntimeException e) { throw invalid(source, e); }
        }
        List<RuleFile> rules = new ArrayList<>();
        for (var entry : files(resources, "rules")) {
            String source = source(entry);
            try {
                RuleDocument document = read(entry.getValue(), RuleDocument.class);
                Style.require(document.schemaVersion() == 1, "schemaVersion must be 1");
                Style.require(document.rules() != null && document.rules().size() <= 4096, "rules must be an array (at most 4096)");
                // Keep null entries until validation so errors include the file that supplied them.
                rules.add(new RuleFile(source, Collections.unmodifiableList(new ArrayList<>(document.rules()))));
            } catch (IOException | RuntimeException e) { throw invalid(source, e); }
        }
        return new PackDefinitions(Collections.unmodifiableMap(styles), List.copyOf(rules));
    }

    public List<SourcedRule> mergeRules(Settings local, Set<String> availableStyles) {
        List<SourcedRule> merged = new ArrayList<>();
        // Stable priority sorting later gives local rules precedence on ties, then pack file path and array order.
        for (Settings.Rule rule : local.rules()) merged.add(new SourcedRule("config.json", rule));
        for (RuleFile file : ruleFiles) {
            try { Settings.validateRules(file.rules(), availableStyles); }
            catch (RuntimeException e) { throw invalid(file.source(), e); }
            for (Settings.Rule rule : file.rules()) merged.add(new SourcedRule(file.source(), rule));
        }
        Style.require(merged.size() <= 4096, "local and resource-pack rules combined must not exceed 4096");
        return List.copyOf(merged);
    }

    private static List<Map.Entry<Identifier, Resource>> files(ResourceManager manager, String directory) {
        // findResources returns only the winning resource for each path, respecting pack order and filters.
        return manager.findResources(directory, id -> id.getNamespace().equals("tooltipstudio")
                        && id.getPath().endsWith(".json")).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString())).toList();
    }

    private static String source(Map.Entry<Identifier, Resource> entry) {
        return "pack '" + entry.getValue().getResourcePackName() + "' / " + entry.getKey();
    }

    private static <T> T read(Resource resource, Class<T> type) throws IOException {
        try (Reader reader = resource.getReader()) {
            T result = GSON.fromJson(reader, type);
            Style.require(result != null, "JSON cannot be null");
            return result;
        }
    }

    private static IllegalArgumentException invalid(String source, Exception cause) {
        return new IllegalArgumentException(source + ": " + cause.getMessage(), cause);
    }
}
