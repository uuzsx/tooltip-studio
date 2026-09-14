package dev.tooltipstudio.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Compiled once per reload. Paths are relative to ItemStack.getNbt(), and matching never writes NBT. */
public final class NbtMatcher {
    private final NbtPathArgumentType.NbtPath path;
    private final List<Predicate<NbtElement>> alternatives;

    private NbtMatcher(NbtPathArgumentType.NbtPath path, List<Predicate<NbtElement>> alternatives) {
        this.path = path;
        this.alternatives = List.copyOf(alternatives);
    }

    public static List<NbtMatcher> compile(Map<String, JsonElement> conditions) {
        Settings.validateNbt(conditions);
        if (conditions == null || conditions.isEmpty()) return List.of();
        List<NbtMatcher> result = new ArrayList<>();
        conditions.forEach((source, values) -> {
            try {
                StringReader reader = new StringReader(source);
                NbtPathArgumentType.NbtPath path = NbtPathArgumentType.nbtPath().parse(reader);
                // The command parser may stop at whitespace or accept a trailing dot; a config key must be complete.
                Style.require(!reader.canRead() && !source.endsWith("."), "path must be complete");
                List<Predicate<NbtElement>> alternatives = new ArrayList<>();
                if (values.isJsonArray()) values.getAsJsonArray().forEach(v -> alternatives.add(valueTest(v.getAsJsonPrimitive())));
                else alternatives.add(valueTest(values.getAsJsonPrimitive()));
                result.add(new NbtMatcher(path, alternatives));
            } catch (CommandSyntaxException | RuntimeException e) {
                throw new IllegalArgumentException("invalid nbt path/value '" + source + "': " + e.getMessage(), e);
            }
        });
        return List.copyOf(result);
    }

    public boolean matches(NbtCompound root) {
        if (root == null) return false;
        try {
            for (NbtElement value : path.get(root)) {
                for (Predicate<NbtElement> alternative : alternatives) {
                    if (alternative.test(value)) return true;
                }
            }
            return false;
        } catch (CommandSyntaxException missingPath) {
            // Missing keys, a different type along the path, and absent list elements are ordinary non-matches.
            return false;
        }
    }

    private static Predicate<NbtElement> valueTest(JsonPrimitive expected) {
        if (expected.isString()) {
            String text = expected.getAsString();
            return actual -> actual instanceof NbtString && actual.asString().equals(text);
        }
        if (expected.isBoolean()) {
            byte value = (byte) (expected.getAsBoolean() ? 1 : 0);
            return actual -> actual instanceof NbtByte number && number.byteValue() == value;
        }
        BigDecimal number = expected.getAsBigDecimal();
        return actual -> {
            if (!(actual instanceof AbstractNbtNumber numeric)) return false;
            try { return new BigDecimal(numeric.numberValue().toString()).compareTo(number) == 0; }
            catch (NumberFormatException nonFinite) { return false; }
        };
    }
}
