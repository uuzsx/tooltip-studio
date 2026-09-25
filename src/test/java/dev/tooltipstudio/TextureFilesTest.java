package dev.tooltipstudio;

import dev.tooltipstudio.config.TextureFiles;
import net.minecraft.resource.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class TextureFilesTest {
    private static final String PATH = "textures/styles/valley/reverie_fireR.png";
    private static final String REF = "tooltipstudio:" + PATH;
    private static final String META = "{\"pack\":{\"pack_format\":22,\"description\":\"test\"}}";
    @TempDir Path directory;
    private ResourcePack zip(String name, Map<String, String> entries) throws Exception {
        Path file = directory.resolve(name + ".zip");
        try (var out = new ZipOutputStream(Files.newOutputStream(file))) {
            if (!entries.containsKey("pack.mcmeta")) {
                out.putNextEntry(new ZipEntry("pack.mcmeta")); out.write(META.getBytes(StandardCharsets.UTF_8)); out.closeEntry();
            }
            for (var entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey())); out.write(entry.getValue().getBytes(StandardCharsets.UTF_8)); out.closeEntry();
            }
        }
        return TestPacks.zip(name, file);
    }
    private String read(ResourceManager manager, String reference) throws Exception {
        try (var in = TextureFiles.open(manager, directory, reference)) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }
    @Test void friendsExactMixedCaseFilenameLoadsFromCaseSensitiveZip() throws Exception {
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(zip("friend", Map.of("assets/tooltipstudio/" + PATH, "uppercase R"))))) {
            assertEquals("uppercase R", read(resources, REF));
            assertEquals("uppercase R", read(resources, "TooltipStudio:" + PATH));
            assertThrows(IOException.class, () -> read(resources, "tooltipstudio:textures/styles/valley/missingR.png"));
        }
    }
    @Test void uppercaseFoldersAndExtensionArePreserved() throws Exception {
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(zip("caps", Map.of("assets/tooltipstudio/Textures/Valley/FireR.PNG", "caps"))))) {
            assertEquals("caps", read(resources, "tooltipstudio:Textures/Valley/FireR.PNG"));
        }
    }
    @Test void mixedCaseReferenceCanStillUseExistingLowercaseFile() throws Exception {
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(zip("lower", Map.of("assets/tooltipstudio/" + PATH.toLowerCase(java.util.Locale.ROOT), "lower"))))) {
            assertEquals("lower", read(resources, REF));
        }
    }
    @Test void exactCaseWinsWithinOnePackAndHigherPacksWinBeforeLowerExactCase() throws Exception {
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(zip("both", Map.of("assets/tooltipstudio/" + PATH, "exact",
                        "assets/tooltipstudio/" + PATH.toLowerCase(java.util.Locale.ROOT), "lower"))))) {
            assertEquals("exact", read(resources, REF));
        }
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(zip("low", Map.of("assets/tooltipstudio/" + PATH, "low exact")),
                        zip("high", Map.of("assets/tooltipstudio/" + PATH.toLowerCase(java.util.Locale.ROOT), "high lower"))))) {
            assertEquals("high lower", read(resources, REF));
        }
    }
    @Test void packFiltersStillBlockLowerMixedCaseTextures() throws Exception {
        String filter = """
                {"pack":{"pack_format":22,"description":"filter"},
                 "filter":{"block":[{"namespace":"tooltipstudio","path":"textures/styles/valley/reverie_firer[.]png"}]}}
                """;
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(zip("filtered-low", Map.of("assets/tooltipstudio/" + PATH, "low")),
                        zip("filter", Map.of("pack.mcmeta", filter))))) {
            assertThrows(IOException.class, () -> read(resources, REF));
        }
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(zip("filter-own", Map.of("pack.mcmeta", filter, "assets/tooltipstudio/" + PATH, "own"))))) {
            assertEquals("own", read(resources, REF));
        }
    }
    @Test void directoryExtensionReadsUppercasePathsAndUsesLowercaseFallback() throws Exception {
        Path file = directory.resolve("assets/tooltipstudio/Textures/FireR.PNG");
        Files.createDirectories(file.getParent()); Files.writeString(file, "directory");
        try (var input = TextureFiles.openDirectory(directory, "tooltipstudio", "Textures/FireR.PNG").get()) {
            assertEquals("directory", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        file = directory.resolve("assets/tooltipstudio/textures/lower.png");
        Files.createDirectories(file.getParent()); Files.writeString(file, "lower");
        assertNotNull(TextureFiles.openDirectory(directory, "tooltipstudio", "Textures/Lower.PNG"));
    }
    @Test void localPrefixIgnoresCaseButRetainsActualFilenameAndConfinement() throws Exception {
        Path base = directory.resolve("local"), png = base.resolve("Valley/FireR.PNG");
        Files.createDirectories(png.getParent()); Files.writeString(png, "local");
        try (var input = TextureFiles.open(null, base, "LOCAL:Valley/FireR.PNG")) {
            assertEquals("local", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThrows(IllegalArgumentException.class, () -> TextureFiles.open(null, base, "LoCaL:../outside.png"));
    }
    @Test void mixedCasePathsCannotTraverseOutsidePackOrChangePathSyntax() {
        for (String path : List.of("../Fire.png", "Folder/../Fire.png", "Folder/./Fire.png", "/Fire.png", "Folder//Fire.png", "Folder\\Fire.png"))
            assertThrows(IllegalArgumentException.class, () -> TextureFiles.openDirectory(directory, "tooltipstudio", path));
        assertThrows(IllegalArgumentException.class, () -> TextureFiles.openDirectory(directory, "..", "Fire.png"));
    }
}
