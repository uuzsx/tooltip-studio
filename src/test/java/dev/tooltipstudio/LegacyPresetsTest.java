package dev.tooltipstudio;

import com.google.gson.Gson;
import dev.tooltipstudio.config.LegacyPresets;
import dev.tooltipstudio.config.Settings;
import dev.tooltipstudio.config.Style;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPresetsTest {
    @Test void missingRetiredIdsUseDefaultButCustomAndUnknownNamesArePreserved() {
        assertEquals("default", LegacyPresets.resolve("rare", Set.of("default")));
        assertEquals("rare", LegacyPresets.resolve("rare", Set.of("default", "rare")));
        assertEquals("my_server/rare", LegacyPresets.resolve("my_server/rare", Set.of("default")));
        assertEquals("missing", LegacyPresets.resolve("missing", Set.of("default")));
    }

    @Test void settingsConversionPreservesConditionsAndDoesNotChangeInput() {
        var rule = new Settings.Rule("legendary", 90, List.of("minecraft:netherite_*"), null, null, null);
        var original = new Settings(1, false, "rare", "CustomStyle", List.of(rule));
        var resolved = LegacyPresets.settings(original, Set.of("default"));
        resolved.validate(Set.of("default"));
        assertEquals("default", resolved.defaultStyle());
        assertEquals("default", resolved.rules().get(0).style());
        assertEquals(rule.items(), resolved.rules().get(0).items());
        assertEquals(90, resolved.rules().get(0).priority());
        assertFalse(resolved.enabled());
        assertEquals("CustomStyle", resolved.nbtStyleKey());
        assertEquals("rare", original.defaultStyle());
        assertEquals("legendary", original.rules().get(0).style());
    }

    @Test void customDefinitionWithOldNameIsNeverRecognizedAsAnUntouchedPreset() throws Exception {
        var style = new Gson().fromJson(Files.readString(Path.of("src/main/resources/assets/tooltipstudio/defaults/styles/default.json")), Style.class);
        assertFalse(LegacyPresets.unchanged("rare", style));
        assertFalse(LegacyPresets.unchanged("default", style));
    }
}
