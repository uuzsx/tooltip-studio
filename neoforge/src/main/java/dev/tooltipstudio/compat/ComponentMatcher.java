package dev.tooltipstudio.compat;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import dev.tooltipstudio.config.NbtMatcher;
import dev.tooltipstudio.config.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Matches the actual component's codec representation, including modded persistent components. */
public final class ComponentMatcher {
    private ComponentMatcher() {}
    private record Condition(DataComponentType<?> type, List<NbtMatcher> paths) {
        boolean matches(ItemStack stack, DynamicOps<Tag> ops) {
            Tag value = encode(stack, type, ops);
            if (value == null) return false;
            CompoundTag root = new CompoundTag();
            root.put("value", value);
            return paths.stream().allMatch(path -> path.matches(root));
        }
    }
    public static Predicate<ItemStack> compile(Map<String, JsonElement> conditions) {
        Settings.validateComponents(conditions);
        if (conditions == null || conditions.isEmpty()) return stack -> true;
        List<Condition> result = new ArrayList<>();
        conditions.forEach((id, expected) -> {
            var key = VersionApi.id(id);
            if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(key))
                throw new IllegalArgumentException("unknown component: " + id);
            DataComponentType<?> type = VersionApi.component(key);
            if (type.codec() == null) throw new IllegalArgumentException("component has no persistent codec: " + id);
            Map<String, JsonElement> paths = new LinkedHashMap<>();
            if (expected.isJsonObject()) expected.getAsJsonObject().asMap().forEach((path, value) ->
                    paths.put("value" + (path.startsWith("[") ? "" : ".") + path, value));
            else paths.put("value", expected);
            result.add(new Condition(type, NbtMatcher.compile(paths)));
        });
        var compiled = List.copyOf(result);
        return stack -> {
            var client = Minecraft.getInstance();
            // Registry-backed components use the connected world's dynamic registries.
            DynamicOps<Tag> ops = client != null && client.level != null
                    ? RegistryOps.create(NbtOps.INSTANCE, client.level.registryAccess()) : NbtOps.INSTANCE;
            return compiled.stream().allMatch(condition -> condition.matches(stack, ops));
        };
    }
    private static <T> Tag encode(ItemStack stack, DataComponentType<T> type, DynamicOps<Tag> ops) {
        T value = stack.get(type);
        if (value == null) return null;
        // Unsupported or unencodable data is an ordinary non-match, never a tooltip-render crash.
        try { return type.codec().encodeStart(ops, value).result().orElse(null); }
        catch (RuntimeException unavailableRegistry) { return null; }
    }
}
