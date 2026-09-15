package dev.tooltipstudio;

import com.google.gson.Gson;
import dev.tooltipstudio.config.Settings;
import dev.tooltipstudio.config.Style;
import dev.tooltipstudio.render.Slices;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {
    private final Gson gson = new Gson();
    private <T> T resource(String name, Class<T> type) throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/tooltipstudio/defaults/" + name), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        }
    }

    @Test void allBundledStylesAndRulesAreValid() throws Exception {
        var names = resource("styles.json", String[].class);
        assertEquals(9, names.length);
        for (String name : names) {
            var style = resource("styles/" + name + ".json", Style.class);
            style.validate();
            assertEquals(0, style.offsetX(), "old JSON defaults to no horizontal offset");
            assertEquals(0, style.offsetY(), "old JSON defaults to no vertical offset");
        }
        resource("config.json", Settings.class).validate(new HashSet<>(List.of(names)));
    }

    @Test void fixedCapsRemainPixelExactAcrossWidths() {
        for (int width = 9; width <= 2048; width++) {
            var pieces = Slices.axis(18, 3, 5, width);
            assertEquals(3, pieces.get(0).targetSize());
            assertEquals(5, pieces.get(2).targetSize());
            assertEquals(13, pieces.get(2).source());
            assertEquals(width - 5, pieces.get(2).target());
            assertEquals(width, pieces.stream().mapToInt(Slices.Segment::targetSize).sum());
            assertEquals(pieces.get(1).target(), pieces.get(0).target() + pieces.get(0).targetSize());
            assertEquals(pieces.get(2).target(), pieces.get(1).target() + pieces.get(1).targetSize());
        }
    }

    @Test void badSlicesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Slices.axis(8, 4, 4, 100));
        assertThrows(IllegalArgumentException.class, () -> Slices.axis(18, 4, 4, 7));
        assertThrows(IllegalArgumentException.class, () -> Slices.axis(18, -1, 4, 100));
    }

    @Test void malformedAtlasAndDividerAreRejected() throws Exception {
        var style = resource("styles/rare.json", Style.class);
        var json = gson.toJsonTree(style).getAsJsonObject();
        json.getAsJsonObject("background").addProperty("u", 128);
        assertThrows(IllegalArgumentException.class, () -> gson.fromJson(json, Style.class).validate());
        var divider = gson.toJsonTree(style).getAsJsonObject();
        divider.getAsJsonObject("separator").addProperty("leftCap", 18);
        assertThrows(IllegalArgumentException.class, () -> gson.fromJson(divider, Style.class).validate());
    }

    @Test void itemGlobsAreAnchoredAndEscapeRegexCharacters() {
        assertTrue(Settings.glob("minecraft:netherite_*").matcher("minecraft:netherite_sword").matches());
        assertFalse(Settings.glob("minecraft:netherite_*").matcher("mod:minecraft:netherite_sword").matches());
        assertTrue(Settings.glob("*:gem.*").matcher("test:gem.red").matches());
        assertFalse(Settings.glob("*:gem.*").matcher("test:gemXred").matches());
        assertTrue(Settings.glob("*:*gem*").matcher("test:red_gemstone").matches());
    }

    @Test void optionalTooltipOffsetsLoadIndependentlyAndRejectExtremeValues() throws Exception {
        var json = gson.toJsonTree(resource("styles/rare.json", Style.class)).getAsJsonObject();
        json.remove("offsetX");
        json.addProperty("offsetY", -12);
        var up = gson.fromJson(json, Style.class);
        up.validate();
        assertEquals(0, up.offsetX());
        assertEquals(-12, up.offsetY());
        json.addProperty("offsetX", 24);
        json.addProperty("offsetY", 16);
        var downRight = gson.fromJson(json, Style.class);
        downRight.validate();
        assertEquals(24, downRight.offsetX());
        assertEquals(16, downRight.offsetY());
        for (String field : List.of("offsetX", "offsetY")) {
            for (int value : new int[]{-4096, 4096}) {
                var valid = json.deepCopy();
                valid.addProperty(field, value);
                assertDoesNotThrow(() -> gson.fromJson(valid, Style.class).validate());
            }
            for (int value : new int[]{-4097, 4097, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                var invalid = json.deepCopy();
                invalid.addProperty(field, value);
                assertThrows(IllegalArgumentException.class, () -> gson.fromJson(invalid, Style.class).validate());
            }
        }
    }

    @Test void unknownStylesAndEmptyRulesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Settings(1, true, "missing", "TooltipStyle", List.of()).validate(java.util.Set.of("rare")));
        assertThrows(IllegalArgumentException.class,
                () -> new Settings(1, true, "rare", "TooltipStyle", List.of(
                        new Settings.Rule("rare", 0, null, null, null, null))).validate(java.util.Set.of("rare")));
    }
}
