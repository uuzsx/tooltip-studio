package dev.tooltipstudio;

import com.google.gson.Gson;
import dev.tooltipstudio.config.Settings;
import dev.tooltipstudio.config.LegacyPresets;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ComponentConfigurationTest {
    private Settings.Rule rule(String components) {
        return new Gson().fromJson("{\"style\":\"default\",\"components\":" + components + "}", Settings.Rule.class);
    }
    private void validate(String value) { Settings.validateRules(List.of(rule(value)), Set.of("default")); }
    @Test void componentOnlyRulesAcceptScalarsAndNestedPaths() {
        validate("""
            {"minecraft:rarity":["rare","epic"], "minecraft:damage":0,
             "minecraft:custom_data":{"Monumenta.Location":"forest","Monumenta.Tier":["artifact","rare"]}}
            """);
    }
    @Test void emptyRulesAreStillRejected() {
        assertThrows(IllegalArgumentException.class, () -> validate("{}"));
    }
    @Test void componentIdsMustBeNamespacedAndLowercase() {
        for (String id : List.of("rarity", "minecraft:Rarity", "minecraft:", " minecraft:rarity"))
            assertThrows(IllegalArgumentException.class, () -> validate("{\""+id+"\":\"epic\"}"));
    }
    @Test void emptyNullOrNestedObjectExpectedValuesAreRejected() {
        for (String value : List.of("null", "{}", "[]", "[{}]", "{\"a\":null}", "{\"a\":{\"b\":1}}"))
            assertThrows(IllegalArgumentException.class, () -> validate("{\"minecraft:custom_data\":"+value+"}"));
    }
    @Test void decorationRulesRetainComponentConditions() {
        var d = new Gson().fromJson("""
            {"decorations":["badge"],"components":{"minecraft:rarity":"epic"}}
            """, Settings.DecorationRule.class);
        Settings.validateDecorationRules(List.of(d), Set.of("badge"));
        assertEquals(d.components(), d.condition().components());
    }
    @Test void legacyStyleResolutionDoesNotDiscardComponents() {
        var original = rule("{\"minecraft:rarity\":\"epic\"}");
        assertEquals(original.components(), LegacyPresets.rules(List.of(original), Set.of("default")).get(0).components());
    }
    @Test void olderJsonStillHasNoComponentConditions() {
        var old = new Gson().fromJson("""
            {"style":"default","nbt":{"Monumenta.Location":"forest"}}
            """, Settings.Rule.class);
        assertNull(old.components());
        Settings.validateRules(List.of(old), Set.of("default"));
    }
}
