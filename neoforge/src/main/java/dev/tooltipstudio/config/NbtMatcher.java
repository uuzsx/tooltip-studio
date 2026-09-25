package dev.tooltipstudio.config;

import dev.tooltipstudio.compat.VersionApi;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Compiled once per reload. Paths are relative to legacy item NBT or modern minecraft:custom_data; matching never writes data. */
public final class NbtMatcher {
    private final NbtPathArgument.NbtPath path;
    private final List<Predicate<Tag>> alternatives;

    private NbtMatcher(NbtPathArgument.NbtPath path, List<Predicate<Tag>> alternatives) {
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
                NbtPathArgument.NbtPath path = NbtPathArgument.nbtPath().parse(reader);
                // The command parser may stop at whitespace or accept a trailing dot; a config key must be complete.
                Style.require(!reader.canRead() && !source.endsWith("."), "path must be complete");
                List<Predicate<Tag>> alternatives = new ArrayList<>();
                if (values.isJsonArray()) values.getAsJsonArray().forEach(v -> alternatives.add(valueTest(v.getAsJsonPrimitive())));
                else alternatives.add(valueTest(values.getAsJsonPrimitive()));
                result.add(new NbtMatcher(path, alternatives));
            } catch (CommandSyntaxException | RuntimeException e) {
                throw new IllegalArgumentException("invalid nbt path/value '" + source + "': " + e.getMessage(), e);
            }
        });
        return List.copyOf(result);
    }

    public boolean matches(CompoundTag root) {
        if (root == null) return false;
        try {
            for (Tag value : path.get(root)) {
                for (Predicate<Tag> alternative : alternatives) {
                    if (alternative.test(value)) return true;
                }
            }
            return false;
        } catch (CommandSyntaxException missingPath) {
            // Missing keys, a different type along the path, and absent list elements are ordinary non-matches.
            return false;
        }
    }

    private static Predicate<Tag> valueTest(JsonPrimitive expected) {
        if (expected.isString()) {
            String text = expected.getAsString();
            return actual -> actual instanceof StringTag && VersionApi.string((StringTag) actual).equals(text);
        }
        if (expected.isBoolean()) {
            byte value = (byte) (expected.getAsBoolean() ? 1 : 0);
            return actual -> actual instanceof ByteTag number && VersionApi.byteValue(number) == value;
        }
        BigDecimal number = expected.getAsBigDecimal();
        return actual -> {
            if (!(actual instanceof NumericTag numeric)) return false;
            try { return new BigDecimal(VersionApi.number(numeric).toString()).compareTo(number) == 0; }
            catch (NumberFormatException nonFinite) { return false; }
        };
    }
}
