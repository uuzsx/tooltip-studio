package dev.tooltipstudio.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConfigManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("Tooltip Studio");
    private static final Gson GSON = new GsonBuilder().create();
    private final Path directory = FabricLoader.getInstance().getConfigDir().resolve("tooltipstudio");
    private Snapshot current;
    private long generation;
    private String lastError;

    public record LoadedStyle(Style style, Identifier texture, Map<Integer, Identifier> inlineTextures,
                              List<LoadedDecoration> decorations) {}
    public record LoadedDecoration(String id, DecorationDefinition definition, Identifier texture) {}
    private record Atlas(String texture, int width, int height) {}
    private record CompiledRule(String style, List<Pattern> items, List<TagKey<Item>> tags, List<String> rarities,
                                List<NbtMatcher> nbt) {
        boolean matches(ItemStack stack, String id) {
            // OR inside one array; AND between supplied arrays.
            return (items.isEmpty() || items.stream().anyMatch(p -> p.matcher(id).matches()))
                    && (tags.isEmpty() || tags.stream().anyMatch(stack::isIn))
                    && (rarities.isEmpty() || rarities.contains(stack.getRarity().name().toLowerCase(Locale.ROOT)))
                    && (nbt.isEmpty() || nbt.stream().allMatch(condition -> condition.matches(stack.getNbt())));
        }
    }
    private record CompiledDecorationRule(List<String> decorations, CompiledRule condition) {}
    private record Snapshot(Settings settings, Map<String, LoadedStyle> styles, List<CompiledRule> rules,
                            Map<String, LoadedDecoration> decorations, List<CompiledDecorationRule> decorationRules,
                            List<Identifier> ownedTextures) {}

    public void initialize() {
        try {
            Files.createDirectories(directory.resolve("styles"));
            Files.createDirectories(directory.resolve("decorations"));
            Files.createDirectories(directory.resolve("textures"));
            // Seed only on first installation. Deleted sample styles stay deleted on later starts.
            if (!Files.exists(directory.resolve("config.json"))) {
                try (Reader reader = new java.io.InputStreamReader(bundled("styles.json"), StandardCharsets.UTF_8)) {
                    for (String name : GSON.fromJson(reader, String[].class))
                        copyDefault("styles/" + name + ".json");
                }
                copyDefault("config.json");
            }
        } catch (Exception e) {
            LOGGER.error("Could not create Tooltip Studio configuration", e);
        }
    }

    private InputStream bundled(String name) throws IOException {
        InputStream stream = ConfigManager.class.getResourceAsStream("/assets/tooltipstudio/defaults/" + name);
        if (stream == null) throw new IOException("Missing bundled default: " + name);
        return stream;
    }
    private void copyDefault(String name) throws IOException {
        Path destination = directory.resolve(name);
        if (!Files.exists(destination)) try (InputStream input = bundled(name)) { Files.copy(input, destination); }
    }

    /** Called on the client apply thread, or by a client command. A bad edit retains the last working snapshot. */
    public boolean reload(ResourceManager resources) {
        List<NativeImage> pendingImages = new ArrayList<>();
        List<Identifier> registered = new ArrayList<>();
        try {
            Settings settings = read(directory.resolve("config.json"), Settings.class);
            PackDefinitions packs = PackDefinitions.load(resources);
            Map<String, Style> definitions = StyleFiles.loadLocal(directory.resolve("styles"), packs.styles().keySet());
            // Ignore only unchanged old preset parameters whose bundled texture was retired.
            // Edited styles and textures supplied again by a resource pack remain the user's choice.
            definitions.entrySet().removeIf(entry -> LegacyPresets.unchanged(entry.getKey(), entry.getValue())
                    && resources.getResource(new Identifier(entry.getValue().texture())).isEmpty());
            definitions.putAll(packs.styles());
            // The base style also exists for upgrades without creating or replacing any user files.
            if (!definitions.containsKey("default")) {
                try (Reader reader = new java.io.InputStreamReader(bundled("styles/default.json"), StandardCharsets.UTF_8)) {
                    Style base = GSON.fromJson(reader, Style.class);
                    Style.require(base != null, "bundled default style cannot be null");
                    base.validate();
                    definitions.put("default", base);
                }
            }
            settings = LegacyPresets.settings(settings, definitions.keySet());
            settings.validate(definitions.keySet());
            List<CompiledRule> rules = packs.mergeRules(settings, definitions.keySet()).stream()
                    .sorted(Comparator.comparingInt((PackDefinitions.SourcedRule r) -> r.rule().priority()).reversed())
                    .map(ConfigManager::compile).toList();
            Map<String, DecorationDefinition> decorations = DecorationFiles.loadLocal(directory.resolve("decorations"), packs.decorations().keySet());
            decorations.putAll(packs.decorations());
            List<CompiledDecorationRule> decorationRules = packs.mergeDecorationRules(settings, decorations.keySet()).stream()
                    .sorted(Comparator.comparingInt((PackDefinitions.SourcedDecorationRule r) -> r.rule().priority()).reversed())
                    .map(r -> new CompiledDecorationRule(List.copyOf(r.rule().decorations()),
                            compile(new PackDefinitions.SourcedRule(r.source(), r.rule().condition())))).toList();
            Map<String, Atlas> atlases = new LinkedHashMap<>();
            definitions.forEach((id, style) -> {
                atlases.put("styles/" + id, new Atlas(style.texture(), style.textureWidth(), style.textureHeight()));
                for (int i = 0; i < style.decorations().size(); i++) {
                    var decoration = style.decorations().get(i);
                    if (!decoration.isText() && decoration.texture() != null)
                        atlases.put("inline/" + id + "/" + i,
                                new Atlas(decoration.texture(), decoration.textureWidth(), decoration.textureHeight()));
                }
            });
            decorations.forEach((id, decoration) -> {
                if (!decoration.isText()) atlases.put("decorations/" + id,
                        new Atlas(decoration.texture(), decoration.textureWidth(), decoration.textureHeight()));
            });
            // Decode and validate every atlas before touching the active textures.
            Map<Atlas, NativeImage> images = new LinkedHashMap<>();
            for (Atlas atlas : new LinkedHashSet<>(atlases.values())) {
                NativeImage image;
                if (atlas.texture().startsWith("local:")) {
                    String relative = atlas.texture().substring(6);
                    Path base = directory.resolve("textures").toAbsolutePath().normalize();
                    Path texture = base.resolve(relative).normalize();
                    Style.require(!relative.isBlank() && texture.startsWith(base)
                            && texture.toRealPath().startsWith(base.toRealPath()), "local texture must stay under textures/");
                    try (InputStream input = Files.newInputStream(texture)) { image = NativeImage.read(input); }
                } else {
                    Identifier id = new Identifier(atlas.texture());
                    try (InputStream input = resources.getResource(id)
                            .orElseThrow(() -> new IOException("Missing texture: " + id)).getInputStream()) {
                        image = NativeImage.read(input);
                    }
                }
                pendingImages.add(image);
                Style.require(image.getWidth() == atlas.width() && image.getHeight() == atlas.height(),
                        atlas.texture() + ": PNG dimensions do not match textureWidth/textureHeight");
                images.put(atlas, image);
            }
            var textures = MinecraftClient.getInstance().getTextureManager();
            Map<Atlas, Identifier> uploaded = new LinkedHashMap<>();
            int index = 0;
            long nextGeneration = ++generation;
            for (var entry : images.entrySet()) {
                Identifier id = new Identifier("tooltipstudio", "runtime/" + nextGeneration + "/atlas_" + index++);
                NativeImage image = entry.getValue();
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                pendingImages.remove(image); // NativeImageBackedTexture now owns it.
                try {
                    texture.setFilter(false, false);
                    textures.registerTexture(id, texture);
                } catch (RuntimeException e) { texture.close(); throw e; }
                registered.add(id);
                uploaded.put(entry.getKey(), id);
            }
            Map<String, Identifier> textureIds = new LinkedHashMap<>();
            atlases.forEach((name, atlas) -> textureIds.put(name, uploaded.get(atlas)));
            Map<String, LoadedStyle> loaded = new LinkedHashMap<>();
            definitions.forEach((id, style) -> {
                Map<Integer, Identifier> inline = new LinkedHashMap<>();
                for (int i = 0; i < style.decorations().size(); i++) {
                    Identifier texture = textureIds.get("inline/" + id + "/" + i);
                    if (texture != null) inline.put(i, texture);
                }
                loaded.put(id, new LoadedStyle(style, textureIds.get("styles/" + id), Map.copyOf(inline), List.of()));
            });
            Map<String, LoadedDecoration> loadedDecorations = new LinkedHashMap<>();
            decorations.forEach((id, decoration) -> loadedDecorations.put(id,
                    new LoadedDecoration(id, decoration, textureIds.get("decorations/" + id))));
            Snapshot previous = current;
            current = new Snapshot(settings, Map.copyOf(loaded), rules, Map.copyOf(loadedDecorations), decorationRules, List.copyOf(registered));
            if (previous != null) previous.ownedTextures.forEach(textures::destroyTexture);
            lastError = null;
            LOGGER.info("Loaded {} tooltip styles, {} style rules, {} decorations and {} decoration rules",
                    loaded.size(), rules.size(), loadedDecorations.size(), decorationRules.size());
            return true;
        } catch (Exception e) {
            pendingImages.forEach(NativeImage::close);
            registered.forEach(MinecraftClient.getInstance().getTextureManager()::destroyTexture);
            lastError = e.getMessage();
            LOGGER.error("Tooltip Studio reload failed; keeping previous configuration: {}", lastError, e);
            return false;
        }
    }

    private static <T> T read(Path path, Class<T> type) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T result = GSON.fromJson(reader, type);
            Style.require(result != null, path.getFileName() + " cannot be null");
            return result;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(path.getFileName() + ": " + e.getMessage(), e);
        }
    }
    private static <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }

    private static CompiledRule compile(PackDefinitions.SourcedRule source) {
        Settings.Rule rule = source.rule();
        try {
            return new CompiledRule(rule.style(), safe(rule.items()).stream().map(Settings::glob).toList(),
                    safe(rule.tags()).stream().map(t -> TagKey.of(RegistryKeys.ITEM, new Identifier(t))).toList(),
                    List.copyOf(safe(rule.rarities())), NbtMatcher.compile(rule.nbt()));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(source.source() + ": " + e.getMessage(), e);
        }
    }

    public LoadedStyle select(ItemStack stack) {
        Snapshot snapshot = current;
        if (snapshot == null || !snapshot.settings.enabled() || stack.isEmpty()) return null;
        LoadedStyle base = selectBase(snapshot, stack);
        if (snapshot.decorationRules.isEmpty()) return base;
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        // All matching rules contribute. Keep each ID once, favoring the highest priority.
        var selected = new LinkedHashSet<String>();
        selection: for (var rule : snapshot.decorationRules) if (rule.condition.matches(stack, id)) {
            for (String decoration : rule.decorations) {
                selected.add(decoration);
                if (selected.size() == 64) break selection;
            }
        }
        if (selected.isEmpty()) return base;
        var overlays = new ArrayList<LoadedDecoration>();
        for (String decoration : selected) overlays.add(snapshot.decorations.get(decoration));
        // Paint low priority first; foreground/background still define the two separate layers.
        Collections.reverse(overlays);
        return new LoadedStyle(base.style(), base.texture(), base.inlineTextures(), List.copyOf(overlays));
    }

    private LoadedStyle selectBase(Snapshot snapshot, ItemStack stack) {
        String key = snapshot.settings.nbtStyleKey();
        if (!key.isEmpty() && stack.hasNbt() && stack.getNbt().contains(key, NbtElement.STRING_TYPE)) {
            LoadedStyle override = snapshot.styles.get(LegacyPresets.resolve(stack.getNbt().getString(key), snapshot.styles.keySet()));
            if (override != null) return override;
        }
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        for (CompiledRule rule : snapshot.rules) if (rule.matches(stack, id)) return snapshot.styles.get(rule.style);
        return snapshot.styles.get(snapshot.settings.defaultStyle());
    }
    public String styleNames() { return current == null ? "" : String.join(", ", current.styles.keySet().stream().sorted().toList()); }
    public String decorationNames() { return current == null ? "" : String.join(", ", current.decorations.keySet().stream().sorted().toList()); }
    public int decorationCount() { return current == null ? 0 : current.decorations.size(); }
    public String lastError() { return lastError; }
    public int count() { return current == null ? 0 : current.styles.size(); }
}
