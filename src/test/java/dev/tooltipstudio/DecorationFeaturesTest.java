package dev.tooltipstudio;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import dev.tooltipstudio.config.*;
import dev.tooltipstudio.render.DecorationLayout;
import dev.tooltipstudio.render.Slices;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecorationFeaturesTest {
    private static final Gson GSON = new Gson();
    @TempDir Path directory;
    private static final String IMAGE = """
            {"texture":"local:sword.png","textureWidth":128,"textureHeight":128,
             "region":{"u":0,"v":0,"width":16,"height":16},"anchor":"BOTTOM_RIGHT","x":6,"y":3}
            """;
    private static final String TEXT = """
            {"type":"text","text":"传说\\nLegendary","color":"#FFD866","anchor":"SEPARATOR_CENTER","foreground":true}
            """;

    private DecorationDefinition image(String anchor, Double xScale, Double yScale) {
        var json = JsonParser.parseString(IMAGE).getAsJsonObject();
        json.addProperty("anchor", anchor); json.addProperty("x", 0); json.addProperty("y", 0);
        if (xScale != null) json.addProperty("x_scale", xScale);
        if (yScale != null) json.addProperty("y_scale", yScale);
        var d = GSON.fromJson(json, DecorationDefinition.class); d.validate(); return d;
    }

    @Test void omittedScalesAndTypePreserveLegacyImageGeometry() {
        for (var anchor : Style.Anchor.values()) if (!anchor.separator()) {
            var d = image(anchor.name(), null, null);
            assertFalse(d.isText()); assertEquals(1f, d.scaleX()); assertEquals(1f, d.scaleY());
            var box = DecorationLayout.place(d, 16, 16, 201, 99, null);
            assertEquals(Slices.anchorX(anchor, 201, 16), box.x());
            assertEquals(Slices.anchorY(anchor, 99, 16), box.y());
        }
    }

    @Test void scalesAreIndependentAndOffsetsStayInGuiPixels() {
        var json = JsonParser.parseString(IMAGE).getAsJsonObject();
        json.addProperty("x_scale", 2); json.addProperty("y_scale", 0.5);
        var d = GSON.fromJson(json, DecorationDefinition.class); d.validate();
        var box = DecorationLayout.place(d, 16, 16, 200, 100, null);
        assertEquals(new DecorationLayout.Box(174, 95, 32, 8), box);
        assertEquals(1f, image("TOP_LEFT", null, 2.0).scaleX());
        assertEquals(1f, image("TOP_LEFT", 2.0, null).scaleY());
    }

    @Test void nonfiniteZeroNegativeAndExcessiveScalesAreRejected() {
        for (String key : List.of("x_scale", "y_scale")) for (double value : new double[]{0, -1, 0.01, 16.01, Double.NaN, Double.POSITIVE_INFINITY}) {
            var json = JsonParser.parseString(IMAGE).getAsJsonObject(); json.addProperty(key, value);
            assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(json, DecorationDefinition.class).validate());
        }
        image("TOP_LEFT", 0.0625, 16.0).validate();
    }

    @Test void allSeparatorAnchorsUseActualLineIncludingInsetsAndHeight() {
        var line = new DecorationLayout.Separator(14, 33, 170, 2);
        assertEquals(new DecorationLayout.Box(14, 26, 16, 16), DecorationLayout.place(image("SEPARATOR_LEFT", null, null), 16, 16, 200, 120, line));
        assertEquals(new DecorationLayout.Box(91, 26, 16, 16), DecorationLayout.place(image("SEPARATOR_CENTER", null, null), 16, 16, 200, 120, line));
        assertEquals(new DecorationLayout.Box(168, 26, 16, 16), DecorationLayout.place(image("SEPARATOR_RIGHT", null, null), 16, 16, 200, 120, line));
        var scaled = DecorationLayout.place(image("SEPARATOR_RIGHT", 2.0, 0.5), 16, 16, 200, 120, line);
        assertEquals(new DecorationLayout.Box(152, 30, 32, 8), scaled);
        var moved = DecorationLayout.place(image("SEPARATOR_RIGHT", 2.0, 0.5), 16, 16, 200, 120,
                new DecorationLayout.Separator(14, 53, 170, 2));
        assertEquals(scaled.y() + 20, moved.y());
    }

    @Test void missingSeparatorHidesOnlySeparatorAnchors() {
        for (String name : List.of("SEPARATOR_LEFT", "SEPARATOR_CENTER", "SEPARATOR_RIGHT"))
            assertNull(DecorationLayout.place(image(name, null, null), 16, 16, 200, 100, null));
        assertNotNull(DecorationLayout.place(image("TOP_LEFT", null, null), 16, 16, 200, 100, null));
    }

    @Test void textNeedsNoAtlasAndLoadsAppearanceOptions() {
        var json = JsonParser.parseString(TEXT).getAsJsonObject();
        json.addProperty("bold", true); json.addProperty("shadow", false); json.addProperty("x_scale", 1.25);
        var d = GSON.fromJson(json, DecorationDefinition.class); d.validate();
        assertTrue(d.isText()); assertTrue(d.isBold()); assertFalse(d.isItalic()); assertFalse(d.hasShadow());
        assertNull(d.texture()); assertNull(d.region()); assertEquals(0xffffd866, d.textColor());
        assertEquals(1.25f, d.scaleX()); assertEquals(1f, d.scaleY()); assertEquals("传说\nLegendary", d.text());
    }

    @Test void invalidTextAndUnknownTypesAreRejected() {
        for (String mutation : List.of("{\"type\":\"unknown\"}", "{\"text\":\"\"}", "{\"text\":null}", "{\"color\":\"gold\"}")) {
            var json = JsonParser.parseString(TEXT).getAsJsonObject();
            JsonParser.parseString(mutation).getAsJsonObject().entrySet().forEach(e -> json.add(e.getKey(), e.getValue()));
            assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(json, DecorationDefinition.class).validate());
        }
        var json = JsonParser.parseString(TEXT).getAsJsonObject();
        json.addProperty("text", "x".repeat(1025));
        assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(json, DecorationDefinition.class).validate());
        json.addProperty("text", "x\n".repeat(16));
        assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(json, DecorationDefinition.class).validate());
    }

    @Test void baseStylesSupportTextAndScaledImageDecorationsAsWell() throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/tooltipstudio/defaults/styles/default.json"))).getAsJsonObject();
        json.getAsJsonArray("decorations").add(JsonParser.parseString(TEXT));
        json.getAsJsonArray("decorations").add(GSON.toJsonTree(image("TOP_RIGHT", 2.0, 0.5)));
        var style = GSON.fromJson(json, Style.class); style.validate();
        assertTrue(style.decorations().get(0).isText()); assertEquals(2f, style.decorations().get(1).scaleX());
    }

    @Test void textOnlyResourcePackDoesNotRequireAnyPng() throws Exception {
        Path definition = directory.resolve("assets/tooltipstudio/decorations/labels/legendary.json");
        Files.createDirectories(definition.getParent()); Files.writeString(definition, TEXT);
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(new DirectoryResourcePack("text-only", directory, false)))) {
            var pack = PackDefinitions.load(resources);
            assertEquals(1, pack.decorations().size()); assertTrue(pack.styles().isEmpty());
            assertTrue(pack.decorations().get("labels/legendary").isText());
        }
    }

    @Test void offsetModeDefaultsToCursorAndAcceptsLegacyScreenMode() throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/tooltipstudio/defaults/styles/default.json"))).getAsJsonObject();
        assertTrue(GSON.fromJson(json, Style.class).cursorRelativeOffset());
        json.addProperty("offsetXMode", "screen");
        var legacy = GSON.fromJson(json, Style.class); legacy.validate(); assertFalse(legacy.cursorRelativeOffset());
        json.addProperty("offsetXMode", "invalid");
        assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(json, Style.class).validate());
    }
}
