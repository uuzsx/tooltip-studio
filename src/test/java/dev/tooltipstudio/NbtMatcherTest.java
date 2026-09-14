package dev.tooltipstudio;

import com.google.gson.Gson;
import dev.tooltipstudio.config.NbtMatcher;
import dev.tooltipstudio.config.Settings;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NbtMatcherTest {
    private Settings settings(String conditions) {
        return new Gson().fromJson("""
                {"schemaVersion":1,"enabled":true,"defaultStyle":"rare","nbtStyleKey":"TooltipStyle",
                 "rules":[{"style":"rare","nbt":%s}]}
                """.formatted(conditions), Settings.class);
    }

    private boolean matches(String conditions, String snbt) throws Exception {
        return matches(conditions, snbt == null ? null : StringNbtReader.parse(snbt));
    }

    private boolean matches(String conditions, NbtCompound root) {
        Settings settings = settings(conditions);
        settings.validate(Set.of("rare"));
        return NbtMatcher.compile(settings.rules().get(0).nbt()).stream().allMatch(m -> m.matches(root));
    }

    @Test void nestedStringsAreExactAndCaseSensitive() throws Exception {
        String condition = """
                {"Monumenta.Location":"forest"}
                """;
        assertTrue(matches(condition, "{Monumenta:{Location:'forest',Tier:'artifact'}}"));
        assertFalse(matches(condition, "{Monumenta:{Location:'Forest'}}"));
        assertFalse(matches(condition, "{Monumenta:{Location:'deep_forest'}}"));
        assertFalse(matches("{\"Monumenta.Location\":\"for*\"}", "{Monumenta:{Location:'forest'}}"));
    }

    @Test void missingAndWrongTypesNeverMatchOrCreateKeys() throws Exception {
        String condition = "{\"Monumenta.Location\":\"forest\"}";
        assertFalse(matches(condition, (NbtCompound) null));
        assertFalse(matches(condition, "{}"));
        assertFalse(matches(condition, "{Monumenta:'forest'}"));
        assertFalse(matches(condition, "{Monumenta:{Location:0}}"));
        assertFalse(matches(condition, "{Monumenta:{Location:['forest']}}"));
        NbtCompound root = StringNbtReader.parse("{Monumenta:{Location:'forest'}}");
        NbtCompound before = root.copy();
        assertTrue(matches(condition, root));
        assertFalse(matches("{\"Monumenta.Missing\":\"\"}", root));
        assertEquals(before, root);
    }

    @Test void alternativesAreOrAndPathsAreAnd() throws Exception {
        String condition = """
                {"Monumenta.Location":["forest","valley"],"Monumenta.Tier":"artifact"}
                """;
        assertTrue(matches(condition, "{Monumenta:{Location:'valley',Tier:'artifact'}}"));
        assertFalse(matches(condition, "{Monumenta:{Location:'forest',Tier:'rare'}}"));
        assertFalse(matches(condition, "{Monumenta:{Location:'city',Tier:'artifact'}}"));
    }

    @Test void plainNamesQuotedKeysAndListPathsWork() throws Exception {
        assertTrue(matches("{\"plain.display.Name\":\"Double Down\"}", "{plain:{display:{Name:'Double Down'}}}"));
        assertTrue(matches("{\"\\\"key.with.dots\\\".Location\":\"forest\"}", "{'key.with.dots':{Location:'forest'}}"));
        assertTrue(matches("{\"Monumenta.MMLore[0]\":\"first\"}", "{Monumenta:{MMLore:['first','second']}}"));
        assertTrue(matches("{\"Monumenta.MMLore[]\":\"second\"}", "{Monumenta:{MMLore:['first','second']}}"));
        assertFalse(matches("{\"Monumenta.MMLore[2]\":\"second\"}", "{Monumenta:{MMLore:['first','second']}}"));
    }

    @Test void numbersRetainPrecisionAndStringsAreNotCoerced() throws Exception {
        assertTrue(matches("{\"CustomModelData\":42}", "{CustomModelData:42}"));
        assertTrue(matches("{\"value\":42.0}", "{value:42s}"));
        assertTrue(matches("{\"value\":9223372036854775807}", "{value:9223372036854775807L}"));
        assertFalse(matches("{\"value\":9223372036854775806}", "{value:9223372036854775807L}"));
        assertFalse(matches("{\"value\":\"42\"}", "{value:42}"));
        assertFalse(matches("{\"value\":42}", "{value:'42'}"));
        assertTrue(matches("{\"flag\":true}", "{flag:1b}"));
        assertTrue(matches("{\"flag\":false}", "{flag:0b}"));
        assertFalse(matches("{\"flag\":true}", "{flag:2b}"));
        assertFalse(matches("{\"flag\":true}", "{flag:1}"));
    }

    @Test void malformedConditionsAndPathsAreRejected() {
        for (String json : new String[]{"{}", "{\"\":\"x\"}", "{\"a\":null}", "{\"a\":{}}", "{\"a\":[]}", "{\"a\":[[\"x\"]]}"}) {
            assertThrows(IllegalArgumentException.class, () -> settings(json).validate(Set.of("rare")), json);
        }
        for (String path : new String[]{"Monumenta..Location", "Monumenta.Location ", "Monumenta.", "Monumenta[", "Monumenta[wat]"}) {
            Settings settings = settings("{\"" + path + "\":\"forest\"}");
            assertThrows(IllegalArgumentException.class, () -> NbtMatcher.compile(settings.rules().get(0).nbt()), path);
        }
    }

    @Test void shippedExampleConfigAndStandaloneRuleAreValid() throws Exception {
        Gson gson = new Gson();
        Settings config = gson.fromJson(Files.readString(Path.of("examples/nbt-matching/config.json")), Settings.class);
        config.validate(Set.of("rare", "uncommon", "epic", "legendary", "mythical"));
        for (Settings.Rule rule : config.rules()) NbtMatcher.compile(rule.nbt());
        Settings.Rule standalone = gson.fromJson(Files.readString(Path.of("examples/nbt-matching/forest-rule.json")), Settings.Rule.class);
        assertEquals(config.rules().get(0), standalone);
        assertTrue(NbtMatcher.compile(standalone.nbt()).get(0).matches(StringNbtReader.parse("{Monumenta:{Location:'forest'}}")));
    }
}
