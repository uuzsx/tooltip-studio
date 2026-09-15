package dev.tooltipstudio;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import dev.tooltipstudio.config.*;
import dev.tooltipstudio.render.Slices;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DecorationTest {
    @TempDir Path directory;
    private static final Gson GSON = new Gson();
    private static final String DEFINITION = """
            {"texture":"tooltipstudio:textures/decorations/sword.png","textureWidth":128,"textureHeight":128,
             "region":{"u":0,"v":0,"width":16,"height":16},"anchor":"TOP_LEFT","x":-6,"y":-6,"foreground":true}
            """;
    private static final Settings LOCAL = new Settings(1, true, "default", "TooltipStyle", List.of());

    private void write(String path, String json) throws Exception {
        var file = directory.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    private LifecycledResourceManagerImpl manager(String... packs) {
        return new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                java.util.Arrays.stream(packs).<net.minecraft.resource.ResourcePack>map(name ->
                        new DirectoryResourcePack(name, directory.resolve(name), false)).toList());
    }

    @Test void oldSettingsAndLegacyConversionKeepDecorationRulesOptional() {
        var old = GSON.fromJson("""
                {"schemaVersion":1,"enabled":true,"defaultStyle":"rare","nbtStyleKey":"TooltipStyle","rules":[]}
                """, Settings.class);
        Settings.validateDecorationRules(old.decorationRules(), Set.of());
        var rule = new Settings.DecorationRule(List.of("sword/left"), 1, List.of("minecraft:*"), null, null, null);
        var updated = new Settings(1, true, "rare", "TooltipStyle", List.of(), List.of(rule));
        assertEquals(List.of(rule), LegacyPresets.settings(updated, Set.of("default")).decorationRules());
    }

    @Test void geometryUsesCroppedRegionAndAnchorsTrackPanelCorners() {
        var d = GSON.fromJson(DEFINITION, DecorationDefinition.class);
        d.validate();
        assertEquals(16, d.region().width());
        assertTrue(d.foreground());
        assertEquals(-6, Slices.anchorX(d.anchor(), 200, 16) + d.x());
        assertEquals(184, Slices.anchorX(Style.Anchor.TOP_RIGHT, 200, 16));
        assertEquals(84, Slices.anchorY(Style.Anchor.BOTTOM_LEFT, 100, 16));
        assertEquals(184, Slices.anchorY(Style.Anchor.BOTTOM_RIGHT, 200, 16));
    }

    @Test void invalidRegionsAnchorsAndOffsetsAreRejected() {
        for (String mutation : List.of("{\"anchor\":\"UNKNOWN\"}", "{\"x\":257}", "{\"y\":-257}",
                "{\"textureWidth\":0}", "{\"region\":{\"u\":127,\"v\":0,\"width\":16,\"height\":16}}")) {
            var json = JsonParser.parseString(DEFINITION).getAsJsonObject();
            JsonParser.parseString(mutation).getAsJsonObject().entrySet().forEach(e -> json.add(e.getKey(), e.getValue()));
            assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(json, DecorationDefinition.class).validate());
        }
    }

    @Test void decorationConditionsShareItemTagRarityAndNbtValidation() {
        var rule = GSON.fromJson("""
                {"decorations":["sword/left"],"items":["minecraft:*"],"tags":["minecraft:planks"],
                 "rarities":["common"],"nbt":{"Monumenta.Location":["forest","valley"]}}
                """, Settings.DecorationRule.class);
        Settings.validateDecorationRules(List.of(rule), Set.of("sword/left"));
        assertEquals(1, NbtMatcher.compile(rule.nbt()).size());
        for (String json : List.of("{\"decorations\":[]}", "{\"decorations\":[\"sword/left\"]}",
                "{\"decorations\":[\"missing\"],\"items\":[\"minecraft:*\"]}",
                "{\"decorations\":[\"sword/left\"],\"nbt\":{\"Monumenta.Location\":{}}}")) {
            assertThrows(IllegalArgumentException.class, () -> Settings.validateDecorationRules(
                    List.of(GSON.fromJson(json, Settings.DecorationRule.class)), Set.of("sword/left")));
        }
    }

    @Test void nestedLocalDefinitionsOverrideByFullIdAndDisappearAfterDeletion() throws Exception {
        write("decorations/sword/left.json", DEFINITION);
        write("decorations/other/left.json", DEFINITION);
        write("decorations/overridden.json", "{broken");
        var loaded = DecorationFiles.loadLocal(directory.resolve("decorations"), Set.of("overridden"));
        assertEquals(Set.of("sword/left", "other/left"), loaded.keySet());
        Files.delete(directory.resolve("decorations/sword/left.json"));
        assertEquals(Set.of("other/left"), DecorationFiles.loadLocal(directory.resolve("decorations"), Set.of("overridden")).keySet());
        assertTrue(DecorationFiles.loadLocal(directory.resolve("absent"), Set.of()).isEmpty());
    }

    @Test void invalidLocalPathsReportDecorationFilename() throws Exception {
        write("decorations/sword/bad.name.json", DEFINITION);
        var error = assertThrows(IllegalArgumentException.class, () -> DecorationFiles.loadLocal(directory.resolve("decorations"), Set.of()));
        assertTrue(error.getMessage().contains("decorations/sword/bad.name.json"));
    }

    @Test void packCanContainOnlyDecorationsAndNoStyleRules() throws Exception {
        write("base/assets/tooltipstudio/decorations/sword/left.json", DEFINITION);
        write("base/assets/tooltipstudio/rules/overlay.json", """
                {"schemaVersion":1,"decorationRules":[{"decorations":["sword/left"],"nbt":{"Monumenta.Location":"forest"}}]}
                """);
        try (var resources = manager("base")) {
            var pack = PackDefinitions.load(resources);
            assertTrue(pack.styles().isEmpty());
            assertTrue(pack.mergeRules(LOCAL, Set.of("default")).isEmpty());
            assertEquals(List.of("sword/left"), pack.mergeDecorationRules(LOCAL, pack.decorations().keySet()).get(0).rule().decorations());
        }
        try (var resources = manager()) {
            assertTrue(PackDefinitions.load(resources).decorations().isEmpty());
        }
    }

    @Test void packOverridesWholeDefinitionAndEmptyRuleFileDisablesLowerRules() throws Exception {
        write("low/assets/tooltipstudio/decorations/sword/left.json", "{broken");
        write("high/assets/tooltipstudio/decorations/sword/left.json", DEFINITION);
        write("low/assets/tooltipstudio/rules/same.json", """
                {"schemaVersion":1,"decorationRules":[{"decorations":["sword/left"],"items":["minecraft:stick"]}]}
                """);
        write("high/assets/tooltipstudio/rules/same.json", "{\"schemaVersion\":1,\"decorationRules\":[]}");
        try (var resources = manager("low", "high")) {
            var pack = PackDefinitions.load(resources);
            assertEquals(-6, pack.decorations().get("sword/left").x());
            assertTrue(pack.mergeDecorationRules(LOCAL, pack.decorations().keySet()).isEmpty());
        }
    }

    @Test void invalidPackReferencesAndLocalTextureIdsReportSource() throws Exception {
        write("bad/assets/tooltipstudio/decorations/left.json", DEFINITION.replace("tooltipstudio:textures/", "local:"));
        try (var resources = manager("bad")) {
            var error = assertThrows(IllegalArgumentException.class, () -> PackDefinitions.load(resources));
            assertTrue(error.getMessage().contains("pack 'bad' / tooltipstudio:decorations/left.json"));
        }
        Files.delete(directory.resolve("bad/assets/tooltipstudio/decorations/left.json"));
        write("bad/assets/tooltipstudio/rules/deco.json", """
                {"schemaVersion":1,"decorationRules":[{"decorations":["missing"],"items":["minecraft:stick"]}]}
                """);
        try (var resources = manager("bad")) {
            var pack = PackDefinitions.load(resources);
            var error = assertThrows(IllegalArgumentException.class, () -> pack.mergeDecorationRules(LOCAL, Set.of()));
            assertTrue(error.getMessage().contains("pack 'bad' / tooltipstudio:rules/deco.json"));
        }
    }

    @Test void shippedPackReferencesOnlyExistingDecorationsAndValidPngRegions() throws Exception {
        var example = Path.of("examples/decoration-pack");
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(new DirectoryResourcePack("example", example, false)))) {
            var pack = PackDefinitions.load(resources);
            assertTrue(pack.styles().isEmpty());
            assertEquals(4, pack.decorations().size());
            assertEquals(3, pack.mergeDecorationRules(LOCAL, pack.decorations().keySet()).size());
            for (var d : pack.decorations().values()) {
                try (var stream = resources.getResource(new net.minecraft.util.Identifier(d.texture())).orElseThrow().getInputStream()) {
                    var image = javax.imageio.ImageIO.read(stream);
                    assertEquals(d.textureWidth(), image.getWidth());
                    assertEquals(d.textureHeight(), image.getHeight());
                }
            }
        }
    }
}
