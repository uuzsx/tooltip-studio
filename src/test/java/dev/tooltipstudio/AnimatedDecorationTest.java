package dev.tooltipstudio;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tooltipstudio.config.*;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedDecorationTest {
    private static final Gson GSON = new Gson();
    @TempDir Path directory;
    private JsonObject decoration() {
        return JsonParser.parseString("""
            {"texture":"tooltipstudio:textures/spark.png","textureWidth":32,"textureHeight":80,
             "region":{"u":8,"v":16,"width":16,"height":16},"anchor":"TOP_LEFT",
             "animation":{"frames":4,"frameTime":2}}
            """).getAsJsonObject();
    }
    private JsonObject style(JsonObject decoration) throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/tooltipstudio/defaults/styles/default.json"))).getAsJsonObject();
        json.getAsJsonArray("decorations").add(decoration); return json;
    }
    @Test void framesUseFirstRegionOffsetsAndLoopAtExactTickBoundaries() {
        var d = GSON.fromJson(decoration(), DecorationDefinition.class); d.validate();
        var animation = d.animation();
        assertEquals(d.region(), animation.regionAt(d.region(), 99));
        assertEquals(new Style.Region(8, 32, 16, 16), animation.regionAt(d.region(), 100));
        assertEquals(new Style.Region(8, 64, 16, 16), animation.regionAt(d.region(), 399));
        assertEquals(d.region(), animation.regionAt(d.region(), 400));
        assertEquals(d.region(), animation.regionAt(d.region(), 4_000_000_000L));
    }
    @Test void horizontalStripAndDefaultTimingAreSupported() {
        var json = decoration(); json.addProperty("textureWidth", 72);
        json.getAsJsonObject("animation").addProperty("direction", "horizontal");
        json.getAsJsonObject("animation").remove("frameTime");
        var d = GSON.fromJson(json, DecorationDefinition.class); d.validate();
        assertEquals(new Style.Region(56, 16, 16, 16), d.animation().regionAt(d.region(), 300));
    }
    @Test void allFramesMustFitEvenWhenFirstFrameIsValid() {
        for (String change : List.of("{\"frames\":5}", "{\"direction\":\"horizontal\"}")) {
            var json = decoration();
            JsonParser.parseString(change).getAsJsonObject().entrySet().forEach(e -> json.getAsJsonObject("animation").add(e.getKey(), e.getValue()));
            assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(json, DecorationDefinition.class).validate());
        }
    }
    @Test void invalidAnimationCannotDivideByZeroOverflowOrRunOnText() {
        for (String invalid : List.of("{}", "{\"frames\":0}", "{\"frames\":257}", "{\"frames\":2147483647}",
                "{\"frames\":4,\"frameTime\":0}", "{\"frames\":4,\"frameTime\":-1}", "{\"frames\":4,\"frameTime\":1201}",
                "{\"frames\":4,\"direction\":\"diagonal\"}")) {
            var json = decoration(); json.add("animation", JsonParser.parseString(invalid));
            assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(json, DecorationDefinition.class).validate());
        }
        var text = decoration(); text.addProperty("type", "text"); text.addProperty("text", "label");
        assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(text, DecorationDefinition.class).validate());
    }
    @Test void inlineTextureDimensionsOverrideBaseAtlasAndStaticOverridesWork() throws Exception {
        var json = decoration(); json.addProperty("textureHeight", 256);
        json.getAsJsonObject("animation").addProperty("frames", 15);
        var s = GSON.fromJson(style(json), Style.class); s.validate();
        assertEquals(256, s.decorations().get(0).imageHeight(s));
        json.remove("animation"); GSON.fromJson(style(json), Style.class).validate();
        json.remove("textureHeight"); var invalid = style(json);
        assertThrows(IllegalArgumentException.class, () -> GSON.fromJson(invalid, Style.class).validate());
    }
    @Test void inheritedAtlasCanAnimateAndOldSerializationHasNoNewFields() throws Exception {
        var json = decoration(); json.remove("texture"); json.remove("textureWidth"); json.remove("textureHeight");
        var s = GSON.fromJson(style(json), Style.class); s.validate();
        assertEquals(128, s.decorations().get(0).imageWidth(s));
        json.remove("animation");
        var d = GSON.fromJson(json, Style.Decoration.class);
        for (String key : List.of("texture", "textureWidth", "textureHeight", "animation"))
            assertFalse(GSON.toJsonTree(d).getAsJsonObject().has(key));
    }
    @Test void packsAcceptSeparateResourcesButRejectLocalInlineTexturesWithFileContext() throws Exception {
        var json = decoration();
        Path file = directory.resolve("assets/tooltipstudio/styles/animated.json");
        Files.createDirectories(file.getParent()); Files.writeString(file, style(json).toString());
        try (var manager = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(TestPacks.directory("animation-test", directory, false)))) {
            assertEquals(4, PackDefinitions.load(manager).styles().get("animated").decorations().get(0).animation().frames());
            json.addProperty("texture", "local:spark.png"); Files.writeString(file, style(json).toString());
            var error = assertThrows(IllegalArgumentException.class, () -> PackDefinitions.load(manager));
            assertTrue(error.getMessage().contains("animated.json"));
            assertTrue(error.getMessage().contains("local:"));
        }
    }
}
