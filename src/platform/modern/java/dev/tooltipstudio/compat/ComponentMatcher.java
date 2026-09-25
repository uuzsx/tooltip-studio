package dev.tooltipstudio.compat;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import dev.tooltipstudio.config.NbtMatcher;
import dev.tooltipstudio.config.Settings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Matches the actual component's codec representation, including modded persistent components. */
public final class ComponentMatcher {
    private ComponentMatcher() {}
    private record Condition(ComponentType<?> type, List<NbtMatcher> paths) {
        boolean matches(ItemStack stack, DynamicOps<NbtElement> ops) {
            NbtElement value = encode(stack, type, ops);
            if (value == null) return false;
            NbtCompound root = new NbtCompound();
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
            if (!Registries.DATA_COMPONENT_TYPE.containsId(key))
                throw new IllegalArgumentException("unknown component: " + id);
            ComponentType<?> type = Registries.DATA_COMPONENT_TYPE.get(key);
            if (type.getCodec() == null) throw new IllegalArgumentException("component has no persistent codec: " + id);
            Map<String, JsonElement> paths = new LinkedHashMap<>();
            if (expected.isJsonObject()) expected.getAsJsonObject().asMap().forEach((path, value) ->
                    paths.put("value" + (path.startsWith("[") ? "" : ".") + path, value));
            else paths.put("value", expected);
            result.add(new Condition(type, NbtMatcher.compile(paths)));
        });
        var compiled = List.copyOf(result);
        return stack -> {
            var client = MinecraftClient.getInstance();
            // Registry-backed components use the connected world's dynamic registries.
            DynamicOps<NbtElement> ops = client != null && client.world != null
                    ? RegistryOps.of(NbtOps.INSTANCE, client.world.getRegistryManager()) : NbtOps.INSTANCE;
            return compiled.stream().allMatch(condition -> condition.matches(stack, ops));
        };
    }
    private static <T> NbtElement encode(ItemStack stack, ComponentType<T> type, DynamicOps<NbtElement> ops) {
        T value = stack.get(type);
        if (value == null) return null;
        // Unsupported or unencodable data is an ordinary non-match, never a tooltip-render crash.
        try { return type.getCodec().encodeStart(ops, value).result().orElse(null); }
        catch (RuntimeException unavailableRegistry) { return null; }
    }
}
