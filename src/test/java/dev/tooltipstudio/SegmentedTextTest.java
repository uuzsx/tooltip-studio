package dev.tooltipstudio;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import dev.tooltipstudio.config.DecorationDefinition;
import dev.tooltipstudio.config.PackDefinitions;
import dev.tooltipstudio.config.Style;
import dev.tooltipstudio.render.DecorationText;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmentedTextTest {
    private static final Gson GSON = new Gson();
    @TempDir Path directory;
    private static final String EXAMPLE = """
            {"type":"text","segments":[
                {"text":"Architect's Ring : ","color":"#555555"},
                {"text":"Artifact","color":"#FF5555"}],
             "anchor":"SEPARATOR_CENTER","foreground":true}
            """;

    private DecorationDefinition parse(String json) {
        var d = GSON.fromJson(json, DecorationDefinition.class); d.validate(); return d;
    }
    private List<Text> lines(String json) { return DecorationText.resolve(parse(json)).lines(); }

    @Test void twoColorsFormOneContinuousLineWithoutAddedSpacing() {
        var d = parse(EXAMPLE);
        var result = DecorationText.resolve(d);
        assertEquals(1, result.lines().size());
        assertEquals("Architect's Ring : Artifact", result.lines().get(0).getString());
        var runs = result.lines().get(0).getSiblings();
        assertEquals(2, runs.size());
        assertEquals(0x555555, runs.get(0).getStyle().getColor().getRgb());
        assertEquals(0xff5555, runs.get(1).getStyle().getColor().getRgb());
        assertFalse(result.italic()); assertNull(d.texture()); assertNull(d.region());
    }

    @Test void omittedStylesInheritOuterDefaultsAndExplicitFalseOverridesThem() {
        var runs = lines("""
                {"type":"text","color":"#ABCDEF","bold":true,"italic":true,"anchor":"BOTTOM",
                 "segments":[{"text":"A"},{"text":"B","color":"#FF0000","bold":false,"italic":false},{"text":"C"}]}
                """).get(0).getSiblings();
        for (int i : new int[]{0, 2}) {
            assertEquals(0xabcdef, runs.get(i).getStyle().getColor().getRgb());
            assertTrue(runs.get(i).getStyle().isBold()); assertTrue(runs.get(i).getStyle().isItalic());
        }
        assertEquals(0xff0000, runs.get(1).getStyle().getColor().getRgb());
        assertFalse(runs.get(1).getStyle().isBold()); assertFalse(runs.get(1).getStyle().isItalic());
    }

    @Test void lineBreaksKeepSegmentStylesAndTrailingEmptyLines() {
        var result = lines("""
                {"type":"text","anchor":"BOTTOM","segments":[
                  {"text":"灰色\\r","color":"#555555"},
                  {"text":"\\n红色\\n","color":"#FF5555"},
                  {"text":"金色\\r\\n","color":"#FFD866"}]}
                """);
        assertEquals(List.of("灰色", "红色", "金色", ""), result.stream().map(Text::getString).toList());
        assertEquals(0xff5555, result.get(1).getSiblings().get(0).getStyle().getColor().getRgb());
        assertEquals(0xffd866, result.get(2).getSiblings().get(0).getStyle().getColor().getRgb());
    }

    @Test void oldStringFormKeepsFormattingAndSerializationOmitsNewField() {
        var d = parse("""
                {"type":"text","text":"旧文字\\n第二行","color":"#12EF34","bold":true,"italic":true,"anchor":"TOP"}
                """);
        assertNull(d.segments()); assertFalse(GSON.toJsonTree(d).getAsJsonObject().has("segments"));
        var result = DecorationText.resolve(d);
        assertEquals(List.of("旧文字", "第二行"), result.lines().stream().map(Text::getString).toList());
        assertTrue(result.italic()); assertTrue(result.lines().get(0).getSiblings().get(0).getStyle().isBold());
        assertEquals(0x12ef34, result.lines().get(0).getStyle().getColor().getRgb());
    }

    @Test void mixedItalicRunsReserveItalicBoundsEvenWhenOuterStyleIsPlain() {
        var d = parse("""
                {"type":"text","segments":[{"text":"A"},{"text":"B","italic":true}],"anchor":"RIGHT"}
                """);
        assertTrue(DecorationText.resolve(d).italic());
        assertFalse(DecorationText.resolve(parse("""
                {"type":"text","italic":true,"segments":[{"text":"B","italic":false}],"anchor":"RIGHT"}
                """)).italic());
    }

    @Test void invalidSegmentsFailInsteadOfSilentlyDroppingContent() {
        for (String segments : List.of("[]", "[null]", "[{}]", "[{\"text\":null}]", "[{\"text\":\" \"}]",
                "[{\"text\":\"X\",\"color\":\"red\"}]")) {
            var json = JsonParser.parseString(EXAMPLE).getAsJsonObject();
            json.add("segments", JsonParser.parseString(segments));
            assertThrows(IllegalArgumentException.class, () -> parse(json.toString()));
        }
        var both = JsonParser.parseString(EXAMPLE).getAsJsonObject(); both.addProperty("text", "ambiguous");
        assertThrows(IllegalArgumentException.class, () -> parse(both.toString()));
        both.remove("text"); both.remove("type");
        assertThrows(IllegalArgumentException.class, () -> parse(both.toString()));
    }

    @Test void aggregateLimitsApplyAcrossAllSegments() {
        var json = JsonParser.parseString(EXAMPLE).getAsJsonObject();
        var segments = json.getAsJsonArray("segments");
        segments.get(0).getAsJsonObject().addProperty("text", "x".repeat(512));
        segments.get(1).getAsJsonObject().addProperty("text", "y".repeat(512));
        parse(json.toString());
        segments.get(1).getAsJsonObject().addProperty("text", "y".repeat(513));
        assertThrows(IllegalArgumentException.class, () -> parse(json.toString()));
        segments.get(0).getAsJsonObject().addProperty("text", "x\n".repeat(8));
        segments.get(1).getAsJsonObject().addProperty("text", "y\n".repeat(8));
        assertThrows(IllegalArgumentException.class, () -> parse(json.toString()));
        segments.get(1).getAsJsonObject().addProperty("text", "y\n".repeat(7));
        parse(json.toString());
        var one = JsonParser.parseString("{\"text\":\"x\"}");
        segments.remove(1); segments.remove(0);
        for (int i = 0; i < 64; i++) segments.add(one.deepCopy());
        parse(json.toString()); segments.add(one.deepCopy());
        assertThrows(IllegalArgumentException.class, () -> parse(json.toString()));
    }

    @Test void styleEmbeddedAndResourcePackDefinitionsUseTheSameSegments() throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/tooltipstudio/defaults/styles/default.json"))).getAsJsonObject();
        json.getAsJsonArray("decorations").add(JsonParser.parseString(EXAMPLE));
        var style = GSON.fromJson(json, Style.class); style.validate();
        assertEquals("Architect's Ring : Artifact", DecorationText.resolve(style.decorations().get(0)).lines().get(0).getString());
        Path file = directory.resolve("assets/tooltipstudio/decorations/labels/artifact.json");
        Files.createDirectories(file.getParent()); Files.writeString(file, EXAMPLE);
        try (var resources = new LifecycledResourceManagerImpl(ResourceType.CLIENT_RESOURCES,
                List.of(TestPacks.directory("segments", directory, false)))) {
            var d = PackDefinitions.load(resources).decorations().get("labels/artifact");
            assertEquals(2, d.segments().size()); assertNull(d.texture());
        }
    }
}
