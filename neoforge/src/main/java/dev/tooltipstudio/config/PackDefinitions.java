package dev.tooltipstudio.config;

import dev.tooltipstudio.compat.VersionApi;
import com.google.gson.Gson;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

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
public record PackDefinitions(Map<String, Style> styles, Map<String, DecorationDefinition> decorations, List<RuleFile> ruleFiles) {
    private static final Gson GSON = new Gson();
    public record RuleFile(String source, List<Settings.Rule> rules, List<Settings.DecorationRule> decorationRules) {}
    public record SourcedRule(String source, Settings.Rule rule) {}
    public record SourcedDecorationRule(String source, Settings.DecorationRule rule) {}
    private record RuleDocument(int schemaVersion, List<Settings.Rule> rules, List<Settings.DecorationRule> decorationRules) {}

    public static PackDefinitions load(ResourceManager resources) throws IOException {
        Map<String, Style> styles = new LinkedHashMap<>();
        for (var entry : files(resources, "styles")) {
            String source = source(entry);
            try {
                String name = StyleFiles.id(entry.getKey().getPath().substring("styles/".length()));
                Style style = read(entry.getValue(), Style.class);
                style.validate();
                Style.require(!TextureFiles.isLocal(style.texture()), "resource-pack styles must use a resource texture ID, not local:");
                for (var decoration : style.decorations())
                    Style.require(decoration.isText() || !TextureFiles.isLocal(decoration.texture()),
                            "resource-pack inline decorations must use a resource texture ID, not local:");
                styles.put(name, style);
            } catch (IOException | RuntimeException e) { throw invalid(source, e); }
        }
        Map<String, DecorationDefinition> decorations = new LinkedHashMap<>();
        for (var entry : files(resources, "decorations")) {
            String source = source(entry);
            try {
                String name = StyleFiles.id(entry.getKey().getPath().substring("decorations/".length()));
                DecorationDefinition definition = read(entry.getValue(), DecorationDefinition.class);
                definition.validate();
                Style.require(definition.isText() || !TextureFiles.isLocal(definition.texture()), "resource-pack decorations must use a resource texture ID, not local:");
                decorations.put(name, definition);
            } catch (IOException | RuntimeException e) { throw invalid(source, e); }
        }
        List<RuleFile> rules = new ArrayList<>();
        for (var entry : files(resources, "rules")) {
            String source = source(entry);
            try {
                RuleDocument document = read(entry.getValue(), RuleDocument.class);
                Style.require(document.schemaVersion() == 1, "schemaVersion must be 1");
                Style.require(document.rules() != null || document.decorationRules() != null,
                        "rules or decorationRules must be an array");
                Style.require(document.rules() == null || document.rules().size() <= 4096, "rules supports at most 4096 entries");
                Style.require(document.decorationRules() == null || document.decorationRules().size() <= 4096,
                        "decorationRules supports at most 4096 entries");
                // Keep null entries until validation so errors include the file that supplied them.
                rules.add(new RuleFile(source, nullableEntries(document.rules()), nullableEntries(document.decorationRules())));
            } catch (IOException | RuntimeException e) { throw invalid(source, e); }
        }
        return new PackDefinitions(Collections.unmodifiableMap(styles), Collections.unmodifiableMap(decorations), List.copyOf(rules));
    }

    private static <T> List<T> nullableEntries(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public List<SourcedRule> mergeRules(Settings local, Set<String> availableStyles) {
        List<SourcedRule> merged = new ArrayList<>();
        // Stable priority sorting later gives local rules precedence on ties, then pack file path and array order.
        for (Settings.Rule rule : local.rules()) merged.add(new SourcedRule("config.json", rule));
        for (RuleFile file : ruleFiles) {
            List<Settings.Rule> rules = LegacyPresets.rules(file.rules(), availableStyles);
            try { Settings.validateRules(rules, availableStyles); }
            catch (RuntimeException e) { throw invalid(file.source(), e); }
            for (Settings.Rule rule : rules) merged.add(new SourcedRule(file.source(), rule));
        }
        Style.require(merged.size() <= 4096, "local and resource-pack rules combined must not exceed 4096");
        return List.copyOf(merged);
    }

    public List<SourcedDecorationRule> mergeDecorationRules(Settings local, Set<String> available) {
        List<SourcedDecorationRule> merged = new ArrayList<>();
        try { Settings.validateDecorationRules(local.decorationRules(), available); }
        catch (RuntimeException e) { throw invalid("config.json / decorationRules", e); }
        for (var rule : nullableEntries(local.decorationRules())) merged.add(new SourcedDecorationRule("config.json", rule));
        for (RuleFile file : ruleFiles) {
            try { Settings.validateDecorationRules(file.decorationRules(), available); }
            catch (RuntimeException e) { throw invalid(file.source(), e); }
            for (var rule : file.decorationRules()) merged.add(new SourcedDecorationRule(file.source(), rule));
        }
        Style.require(merged.size() <= 4096, "local and resource-pack decorationRules combined must not exceed 4096");
        return List.copyOf(merged);
    }

    private static List<Map.Entry<ResourceLocation, Resource>> files(ResourceManager manager, String directory) {
        // findResources returns only the winning resource for each path, respecting pack order and filters.
        return manager.listResources(directory, id -> id.getNamespace().equals("tooltipstudio")
                        && id.getPath().endsWith(".json")).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString())).toList();
    }

    private static String source(Map.Entry<ResourceLocation, Resource> entry) {
        return "pack '" + VersionApi.packName(entry.getValue()) + "' / " + entry.getKey();
    }

    private static <T> T read(Resource resource, Class<T> type) throws IOException {
        try (Reader reader = resource.openAsReader()) {
            T result = GSON.fromJson(reader, type);
            Style.require(result != null, "JSON cannot be null");
            return result;
        }
    }

    private static IllegalArgumentException invalid(String source, Exception cause) {
        return new IllegalArgumentException(source + ": " + cause.getMessage(), cause);
    }
}
