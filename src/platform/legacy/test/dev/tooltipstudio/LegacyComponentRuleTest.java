package dev.tooltipstudio;
import com.google.gson.JsonPrimitive;
import dev.tooltipstudio.compat.ComponentMatcher;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class LegacyComponentRuleTest {
    @Test void componentConditionsCannotSilentlyBecomeUnconditionalOnLegacyMinecraft() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> ComponentMatcher.compile(Map.of("minecraft:rarity", new JsonPrimitive("epic"))));
        assertTrue(error.getMessage().contains("1.20.4"));
    }
    @Test void oldConfigurationsWithoutComponentsKeepWorking() {
        assertTrue(ComponentMatcher.compile(null).test(null));
        assertTrue(ComponentMatcher.compile(Map.of()).test(null));
    }
}
