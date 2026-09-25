package dev.tooltipstudio.config;

import dev.tooltipstudio.compat.VersionApi;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceFilterSection;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Mixed-case texture filenames are an opt-in extension, scoped to Tooltip Studio's PNG reads. */
public final class TextureFiles {
    private TextureFiles() {}

    /** Implemented by directory/overlay packs through mixins; vanilla pack methods stay unchanged. */
    public interface RawTexturePack {
        IoSupplier<InputStream> tooltipstudio$openTexture(String namespace, String path);
    }

    public static boolean isLocal(String reference) {
        return reference != null && reference.regionMatches(true, 0, "local:", 0, 6);
    }

    public static InputStream open(ResourceManager resources, Path localDirectory, String reference) throws IOException {
        if (isLocal(reference)) {
            String relative = reference.substring(6);
            Path base = localDirectory.toAbsolutePath().normalize(), texture = base.resolve(relative).normalize();
            Style.require(!relative.isBlank() && texture.startsWith(base)
                    && texture.toRealPath().startsWith(base.toRealPath()), "local texture must stay under textures/");
            return Files.newInputStream(texture);
        }
        var location = location(reference);
        var canonical = VersionApi.id(location.namespace(), location.path().toLowerCase(Locale.ROOT));
        if (location.path().equals(canonical.getPath())) {
            return resources.getResource(canonical)
                    .orElseThrow(() -> missing(reference)).open();
        }
        // High-priority packs win even when a lower-priority pack has an exact-case filename.
        try (var stream = resources.listPacks()) {
            var packs = stream.toList();
            for (int i = packs.size() - 1; i >= 0; i--) {
                PackResources pack = packs.get(i);
                var input = openPack(pack, location.namespace(), location.path());
                if (input != null) return input.get();
                ResourceFilterSection filter = pack.getMetadataSection(ResourceFilterSection.TYPE);
                if (filter != null && filter.isNamespaceFiltered(location.namespace())
                        && (filter.isPathFiltered(location.path()) || filter.isPathFiltered(canonical.getPath()))) break;
            }
        }
        throw missing(reference);
    }

    private record Location(String namespace, String path) {}
    private static Location location(String reference) {
        int colon = reference.indexOf(':');
        String namespace = colon <= 0 ? "minecraft" : reference.substring(0, colon).toLowerCase(Locale.ROOT);
        String path = colon < 0 ? reference : reference.substring(colon + 1);
        validateNamespace(namespace);
        validatePath(path);
        return new Location(namespace, path);
    }

    private static void validatePath(String path) {
        Style.require(path.matches("[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*"), "invalid texture path: " + path);
        for (String part : path.split("/"))
            Style.require(!part.equals(".") && !part.equals(".."), "texture path must not contain . or ..: " + path);
    }

    private static void validateNamespace(String namespace) {
        Style.require(namespace.matches("[a-z0-9_.-]+") && !namespace.equals(".") && !namespace.equals(".."),
                "invalid texture namespace: " + namespace);
    }

    public static IoSupplier<InputStream> openPack(PackResources pack, String namespace, String path) {
        validateNamespace(namespace);
        validatePath(path);
        if (pack instanceof RawTexturePack raw) return raw.tooltipstudio$openTexture(namespace, path);
        IoSupplier<InputStream> exact;
        try { exact = pack.getRootResource(("assets/" + namespace + "/" + path).split("/")); }
        catch (IllegalArgumentException | UnsupportedOperationException unsupportedPath) { exact = null; }
        return exact != null ? exact : pack.getResource(PackType.CLIENT_RESOURCES,
                VersionApi.id(namespace, path.toLowerCase(Locale.ROOT)));
    }

    public static IoSupplier<InputStream> openDirectory(Path root, String namespace, String path) {
        validatePath(path);
        validateNamespace(namespace);
        Path base = root.resolve("assets").resolve(namespace);
        Path exact = base.resolve(path);
        if (Files.isRegularFile(exact)) return IoSupplier.create(exact);
        Path lowercase = base.resolve(path.toLowerCase(Locale.ROOT));
        return Files.isRegularFile(lowercase) ? IoSupplier.create(lowercase) : null;
    }

    private static IOException missing(String reference) {
        return new IOException("Missing texture: " + reference + " (PNG folder/file case must match texture; also tried lowercase)");
    }
}
