package dev.tooltipstudio;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import dev.tooltipstudio.config.PackDefinitions;
import dev.tooltipstudio.config.Settings;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PackDefinitionsTest {
    @TempDir Path directory;
    private static final Settings LOCAL = new Settings(1, true, "rare", "TooltipStyle", List.of());

    private void write(String pack, String file, String contents) throws Exception {
        Path path = directory.resolve(pack).resolve("assets/tooltipstudio").resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
    }

    private String style(int minWidth) throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/tooltipstudio/defaults/styles/rare.json"))).getAsJsonObject();
        json.addProperty("minWidth", minWidth);
        return json.toString();
    }

    private LifecycledResourceManagerImpl manager(String... packs) {
        return new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                java.util.Arrays.stream(packs).<net.minecraft.resource.ResourcePack>map(name ->
                        new DirectoryResourcePack(name, directory.resolve(name), false)).toList());
    }

    @Test void newStylesAndRulesAreLoadedWithoutEditingLocalSettings() throws Exception {
        write("base", "styles/forest.json", style(125));
        write("base", "rules/forest.json", """
                {"schemaVersion":1,"rules":[{"style":"forest","priority":200,"nbt":{"Monumenta.Location":"forest"}}]}
                """);
        try (var manager = manager("base")) {
            var packs = PackDefinitions.load(manager);
            assertEquals(125, packs.styles().get("forest").minWidth());
            var rules = packs.mergeRules(LOCAL, Set.of("rare", "forest"));
            assertEquals("forest", rules.get(0).rule().style());
            assertEquals("forest", rules.get(0).rule().nbt().get("Monumenta.Location").getAsString());
            assertEquals(0, LOCAL.rules().size());
        }
    }

    @Test void highestPackWinsPerFileAndEmptyRulesReplaceLowerFile() throws Exception {
        write("low", "styles/forest.json", "{broken"); // A shadowed resource is not read.
        write("high", "styles/forest.json", style(150));
        write("low", "rules/same.json", """
                {"schemaVersion":1,"rules":[{"style":"forest","items":["minecraft:stick"]}]}
                """);
        write("high", "rules/same.json", "{\"schemaVersion\":1,\"rules\":[]}");
        write("low", "rules/other.json", """
                {"schemaVersion":1,"rules":[{"style":"forest","items":["minecraft:diamond"]}]}
                """);
        try (var manager = manager("low", "high")) {
            var packs = PackDefinitions.load(manager);
            assertEquals(150, packs.styles().get("forest").minWidth());
            var rules = packs.mergeRules(LOCAL, Set.of("rare", "forest"));
            assertEquals(1, rules.size());
            assertEquals(List.of("minecraft:diamond"), rules.get(0).rule().items());
        }
    }

    @Test void equalPriorityKeepsLocalThenSortedPackFilesAndJsonOrder() throws Exception {
        write("base", "rules/z.json", """
                {"schemaVersion":1,"rules":[{"style":"z","priority":100,"items":["minecraft:stick"]}]}
                """);
        write("base", "rules/a.json", """
                {"schemaVersion":1,"rules":[
                  {"style":"a","priority":100,"items":["minecraft:stick"]},
                  {"style":"second","priority":100,"items":["minecraft:stick"]},
                  {"style":"high","priority":200,"items":["minecraft:stick"]}]}
                """);
        var local = new Settings(1, true, "rare", "TooltipStyle", List.of(
                new Settings.Rule("local", 100, List.of("minecraft:stick"), null, null, null)));
        try (var manager = manager("base")) {
            var rules = PackDefinitions.load(manager).mergeRules(local, Set.of("local", "a", "second", "z", "high"));
            var order = rules.stream().sorted(Comparator.comparingInt((PackDefinitions.SourcedRule r) -> r.rule().priority()).reversed())
                    .map(r -> r.rule().style()).toList();
            assertEquals(List.of("high", "local", "a", "second", "z"), order);
        }
    }

    @Test void nestedPackStylesUseFullPathsForRulesAndPackOverrides() throws Exception {
        write("low", "styles/forest.json", style(100));
        write("low", "styles/monumenta/forest.json", "{broken");
        write("low", "styles/other/region/forest.json", style(140));
        write("high", "styles/monumenta/forest.json", style(160));
        write("low", "rules/monumenta/location.json", """
                {"schemaVersion":1,"rules":[{"style":"monumenta/forest","nbt":{"Monumenta.Location":"forest"}}]}
                """);
        try (var manager = manager("low", "high")) {
            var packs = PackDefinitions.load(manager);
            assertEquals(Set.of("forest", "monumenta/forest", "other/region/forest"), packs.styles().keySet());
            assertEquals(100, packs.styles().get("forest").minWidth());
            assertEquals(160, packs.styles().get("monumenta/forest").minWidth());
            assertEquals(140, packs.styles().get("other/region/forest").minWidth());
            assertEquals("monumenta/forest", packs.mergeRules(LOCAL, packs.styles().keySet()).get(0).rule().style());
        }
    }

    @Test void invalidNestedPackPathReportsFullSource() throws Exception {
        write("bad", "styles/monumenta/forest.extra.json", style(100));
        try (var manager = manager("bad")) {
            var error = assertThrows(IllegalArgumentException.class, () -> PackDefinitions.load(manager));
            assertTrue(error.getMessage().contains("pack 'bad' / tooltipstudio:styles/monumenta/forest.extra.json"));
        }
    }

    @Test void disabledPacksLeaveNoDefinitions() throws Exception {
        write("base", "styles/forest.json", style(125));
        try (var enabled = manager("base"); var disabled = manager()) {
            assertEquals(1, PackDefinitions.load(enabled).styles().size());
            assertTrue(PackDefinitions.load(disabled).styles().isEmpty());
            assertTrue(PackDefinitions.load(disabled).mergeRules(LOCAL, Set.of("rare")).isEmpty());
        }
    }

    @Test void invalidRulesIncludePackAndFileInError() throws Exception {
        write("bad", "rules/problem.json", """
                {"schemaVersion":1,"rules":[{"style":"missing","items":["minecraft:stick"]}]}
                """);
        try (var manager = manager("bad")) {
            var packs = PackDefinitions.load(manager);
            var error = assertThrows(IllegalArgumentException.class, () -> packs.mergeRules(LOCAL, Set.of("rare")));
            assertTrue(error.getMessage().contains("pack 'bad' / tooltipstudio:rules/problem.json"));
        }
        write("bad", "rules/problem.json", "{\"schemaVersion\":99,\"rules\":[]}");
        try (var manager = manager("bad")) { assertThrows(IllegalArgumentException.class, () -> PackDefinitions.load(manager)); }
    }

    @Test void packStylesRejectLocalFilesAndUnrelatedNamespacesAreIgnored() throws Exception {
        var json = JsonParser.parseString(style(100)).getAsJsonObject();
        json.addProperty("texture", "local:private.png");
        write("bad", "styles/forest.json", json.toString());
        try (var manager = manager("bad")) {
            var error = assertThrows(IllegalArgumentException.class, () -> PackDefinitions.load(manager));
            assertTrue(error.getMessage().contains("styles/forest.json"));
        }
        Path unrelated = directory.resolve("other/assets/another_mod/styles/whatever.json");
        Files.createDirectories(unrelated.getParent());
        Files.writeString(unrelated, "{broken");
        try (var manager = manager("other")) { assertTrue(PackDefinitions.load(manager).styles().isEmpty()); }
    }

    @Test void shippedExamplePackDefinesValidStylesTexturesAndRules() throws Exception {
        Path example = Path.of("examples/resource-pack");
        try (var manager = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(new DirectoryResourcePack("example", example, false)))) {
            var pack = PackDefinitions.load(manager);
            assertEquals(Set.of("pack_forest", "pack_wood", "monumenta/forest"), pack.styles().keySet());
            assertEquals(0, pack.styles().get("monumenta/forest").offsetX());
            assertEquals(-12, pack.styles().get("monumenta/forest").offsetY());
            assertEquals(0, pack.styles().get("pack_forest").offsetY());
            assertEquals(3, pack.mergeRules(LOCAL, pack.styles().keySet()).size());
            for (var style : pack.styles().values()) {
                try (var stream = manager.getResource(new net.minecraft.util.Identifier(style.texture())).orElseThrow().getInputStream()) {
                    var image = javax.imageio.ImageIO.read(stream);
                    assertEquals(style.textureWidth(), image.getWidth());
                    assertEquals(style.textureHeight(), image.getHeight());
                }
            }
        }
        var packMeta = new Gson().fromJson(Files.readString(example.resolve("pack.mcmeta")), com.google.gson.JsonObject.class);
        assertEquals(22, packMeta.getAsJsonObject("pack").get("pack_format").getAsInt());
    }
}
