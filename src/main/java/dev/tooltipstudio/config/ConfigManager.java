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

    public record LoadedStyle(Style style, Identifier texture) {}
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
    private record Snapshot(Settings settings, Map<String, LoadedStyle> styles, List<CompiledRule> rules,
                            List<Identifier> ownedTextures) {}

    public void initialize() {
        try {
            Files.createDirectories(directory.resolve("styles"));
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
            definitions.putAll(packs.styles());
            settings.validate(definitions.keySet());
            List<CompiledRule> rules = packs.mergeRules(settings, definitions.keySet()).stream()
                    .sorted(Comparator.comparingInt((PackDefinitions.SourcedRule r) -> r.rule().priority()).reversed())
                    .map(ConfigManager::compile).toList();
            // Decode and validate every atlas before touching the active textures.
            List<NativeImage> images = new ArrayList<>();
            for (Style style : definitions.values()) {
                NativeImage image;
                if (style.texture().startsWith("local:")) {
                    String relative = style.texture().substring(6);
                    Path base = directory.resolve("textures").toAbsolutePath().normalize();
                    Path texture = base.resolve(relative).normalize();
                    Style.require(!relative.isBlank() && texture.startsWith(base)
                            && texture.toRealPath().startsWith(base.toRealPath()), "local texture must stay under textures/");
                    try (InputStream input = Files.newInputStream(texture)) { image = NativeImage.read(input); }
                } else {
                    Identifier id = new Identifier(style.texture());
                    try (InputStream input = resources.getResource(id)
                            .orElseThrow(() -> new IOException("Missing texture: " + id)).getInputStream()) {
                        image = NativeImage.read(input);
                    }
                }
                pendingImages.add(image);
                Style.require(image.getWidth() == style.textureWidth() && image.getHeight() == style.textureHeight(),
                        style.texture() + ": PNG dimensions do not match textureWidth/textureHeight");
                images.add(image);
            }
            var textures = MinecraftClient.getInstance().getTextureManager();
            Map<String, LoadedStyle> loaded = new LinkedHashMap<>();
            int index = 0;
            long nextGeneration = ++generation;
            for (var entry : definitions.entrySet()) {
                Identifier id = new Identifier("tooltipstudio", "runtime/" + nextGeneration + "/" + entry.getKey());
                NativeImage image = images.get(index++);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                pendingImages.remove(image); // NativeImageBackedTexture now owns it.
                try {
                    texture.setFilter(false, false);
                    textures.registerTexture(id, texture);
                } catch (RuntimeException e) { texture.close(); throw e; }
                registered.add(id);
                loaded.put(entry.getKey(), new LoadedStyle(entry.getValue(), id));
            }
            Snapshot previous = current;
            current = new Snapshot(settings, Map.copyOf(loaded), rules, List.copyOf(registered));
            if (previous != null) previous.ownedTextures.forEach(textures::destroyTexture);
            lastError = null;
            LOGGER.info("Loaded {} tooltip styles and {} rules", loaded.size(), rules.size());
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
        String key = snapshot.settings.nbtStyleKey();
        if (!key.isEmpty() && stack.hasNbt() && stack.getNbt().contains(key, NbtElement.STRING_TYPE)) {
            LoadedStyle override = snapshot.styles.get(stack.getNbt().getString(key));
            if (override != null) return override;
        }
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        for (CompiledRule rule : snapshot.rules) if (rule.matches(stack, id)) return snapshot.styles.get(rule.style);
        return snapshot.styles.get(snapshot.settings.defaultStyle());
    }
    public String styleNames() { return current == null ? "" : String.join(", ", current.styles.keySet().stream().sorted().toList()); }
    public String lastError() { return lastError; }
    public int count() { return current == null ? 0 : current.styles.size(); }
}
