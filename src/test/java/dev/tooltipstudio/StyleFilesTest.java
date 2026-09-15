package dev.tooltipstudio;

import com.google.gson.JsonParser;
import dev.tooltipstudio.config.Settings;
import dev.tooltipstudio.config.StyleFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StyleFilesTest {
    @TempDir Path directory;

    private void write(String relative, int width) throws Exception {
        Path path = directory.resolve(relative);
        Files.createDirectories(path.getParent());
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/tooltipstudio/defaults/styles/default.json"))).getAsJsonObject();
        json.addProperty("minWidth", width);
        Files.writeString(path, json.toString());
    }

    @Test void recursiveLocalStylesPreserveFlatIdsAndSeparateSameBasenames() throws Exception {
        write("forest.json", 100);
        write("monumenta/forest.json", 120);
        write("another/region_2/forest.json", 140);
        Files.writeString(directory.resolve("notes.txt"), "not a style");
        Files.createDirectory(directory.resolve("folder.json"));
        var styles = StyleFiles.loadLocal(directory, Set.of());
        assertEquals(Set.of("forest", "monumenta/forest", "another/region_2/forest"), styles.keySet());
        assertEquals(100, styles.get("forest").minWidth());
        assertEquals(120, styles.get("monumenta/forest").minWidth());
        assertEquals(140, styles.get("another/region_2/forest").minWidth());
        var settings = new Settings(1, true, "another/region_2/forest", "TooltipStyle", List.of(
                new Settings.Rule("monumenta/forest", 300, List.of("minecraft:stick"), null, null, null)));
        assertDoesNotThrow(() -> settings.validate(styles.keySet()));
    }

    @Test void packOverridesOnlyTheExactLocalPathAndCanShadowInvalidJson() throws Exception {
        write("forest.json", 100);
        write("monumenta/forest.json", 120);
        write("other/forest.json", 140);
        Files.writeString(directory.resolve("monumenta/forest.json"), "{broken");
        var styles = StyleFiles.loadLocal(directory, Set.of("monumenta/forest"));
        assertEquals(Set.of("forest", "other/forest"), styles.keySet());
        var error = assertThrows(IllegalArgumentException.class, () -> StyleFiles.loadLocal(directory, Set.of("forest")));
        assertTrue(error.getMessage().contains("styles/monumenta/forest.json"));
    }

    @Test void removingNestedFilesRemovesIdsOnNextLoad() throws Exception {
        write("monumenta/forest.json", 120);
        assertEquals(1, StyleFiles.loadLocal(directory, Set.of()).size());
        Files.delete(directory.resolve("monumenta/forest.json"));
        assertTrue(StyleFiles.loadLocal(directory, Set.of()).isEmpty());
    }

    @Test void pathSyntaxRejectsAmbiguousOrAbsoluteReferences() {
        assertEquals("forest", StyleFiles.id("forest.json"));
        assertEquals("monumenta/region_2/forest-green", StyleFiles.id("monumenta/region_2/forest-green.json"));
        for (String invalid : List.of("", ".json", "forest", "Forest.json", "a.b.json", "/forest.json",
                "../forest.json", "a/../forest.json", "a/./forest.json", "a//forest.json", "a\\forest.json",
                "tooltipstudio:forest.json", "C:/forest.json", "a/森林.json", "a/forest.json.json")) {
            assertThrows(IllegalArgumentException.class, () -> StyleFiles.id(invalid), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> StyleFiles.id(null));
    }
}
