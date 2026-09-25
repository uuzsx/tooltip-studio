package dev.tooltipstudio.compat;

import com.google.gson.JsonElement;
import net.minecraft.item.ItemStack;
import java.util.Map;
import java.util.function.Predicate;

public final class ComponentMatcher {
    private ComponentMatcher() {}
    public static Predicate<ItemStack> compile(Map<String, JsonElement> conditions) {
        if (conditions != null && !conditions.isEmpty())
            throw new IllegalArgumentException("components rules require Minecraft 1.21.1+; use nbt on 1.20.4");
        return stack -> true;
    }
}
