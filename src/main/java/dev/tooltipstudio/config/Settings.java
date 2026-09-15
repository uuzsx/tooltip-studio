package dev.tooltipstudio.config;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record Settings(int schemaVersion, boolean enabled, String defaultStyle,
                       String nbtStyleKey, List<Rule> rules, List<DecorationRule> decorationRules) {
    public Settings(int schemaVersion, boolean enabled, String defaultStyle, String nbtStyleKey, List<Rule> rules) {
        this(schemaVersion, enabled, defaultStyle, nbtStyleKey, rules, List.of());
    }
    public record Rule(String style, int priority, List<String> items, List<String> tags,
                       List<String> rarities, Map<String, JsonElement> nbt) {}
    public record DecorationRule(List<String> decorations, int priority, List<String> items, List<String> tags,
                                 List<String> rarities, Map<String, JsonElement> nbt) {
        public Rule condition() { return new Rule(null, priority, items, tags, rarities, nbt); }
    }

    public void validate(Set<String> styles) {
        Style.require(schemaVersion == 1, "schemaVersion must be 1");
        Style.require(styles.contains(defaultStyle), "unknown defaultStyle: " + defaultStyle);
        Style.require(nbtStyleKey != null, "nbtStyleKey is required (empty string disables the override)");
        validateRules(rules, styles);
    }

    public static void validateRules(List<Rule> rules, Set<String> styles) {
        Style.require(rules != null && rules.size() <= 4096, "rules must be an array (at most 4096)");
        for (Rule rule : rules) {
            Style.require(rule != null && styles.contains(rule.style), "rule references an unknown style: " + (rule == null ? "null" : rule.style));
            validateCondition(rule);
        }
    }

    public static void validateDecorationRules(List<DecorationRule> rules, Set<String> decorations) {
        if (rules == null) return; // Optional in configurations written by previous versions.
        Style.require(rules.size() <= 4096, "decorationRules supports at most 4096 rules");
        for (DecorationRule rule : rules) {
            Style.require(rule != null && hasValues(rule.decorations) && rule.decorations.size() <= 64,
                    "decoration rule requires 1..64 decoration IDs");
            for (String id : rule.decorations)
                Style.require(id != null && decorations.contains(id), "rule references an unknown decoration: " + id);
            validateCondition(rule.condition());
        }
    }

    private static void validateCondition(Rule rule) {
        Style.require(hasValues(rule.items) || hasValues(rule.tags) || hasValues(rule.rarities)
                        || (rule.nbt != null && !rule.nbt.isEmpty()),
                "rule requires items, tags, rarities, or nbt");
        validateNbt(rule.nbt);
        if (rule.items != null) for (String item : rule.items)
            Style.require(item != null && item.matches("[a-z0-9_.*-]+:[a-z0-9_./*-]+"), "invalid item pattern: " + item);
        if (rule.tags != null) for (String tag : rule.tags)
            Style.require(tag != null && tag.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), "invalid tag: " + tag);
        if (rule.rarities != null) for (String rarity : rule.rarities)
            Style.require(Set.of("common", "uncommon", "rare", "epic").contains(rarity), "invalid rarity: " + rarity);
    }

    public static boolean hasValues(List<?> list) { return list != null && !list.isEmpty(); }

    public static void validateNbt(Map<String, JsonElement> nbt) {
        if (nbt == null) return;
        Style.require(nbt.size() <= 64, "nbt supports at most 64 paths per rule");
        nbt.forEach((path, value) -> {
            Style.require(path != null && !path.isBlank() && path.length() <= 512,
                    "nbt path must contain 1..512 characters");
            if (value != null && value.isJsonArray()) {
                Style.require(!value.getAsJsonArray().isEmpty() && value.getAsJsonArray().size() <= 64,
                        "nbt " + path + ": alternatives must contain 1..64 values");
                value.getAsJsonArray().forEach(v -> validateNbtValue(path, v));
            } else validateNbtValue(path, value);
        });
    }

    private static void validateNbtValue(String path, JsonElement value) {
        Style.require(value != null && value.isJsonPrimitive(),
                "nbt " + path + ": expected a string, number, boolean, or array of these values");
        Style.require(value.getAsString().length() <= 4096, "nbt " + path + ": value is too long");
        if (value.getAsJsonPrimitive().isNumber()) {
            try { value.getAsBigDecimal(); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("nbt " + path + ": invalid number", e); }
        }
    }

    public static Pattern glob(String input) {
        StringBuilder result = new StringBuilder("^");
        for (String part : input.split("\\*", -1)) {
            if (result.length() > 1) result.append(".*");
            result.append(Pattern.quote(part));
        }
        return Pattern.compile(result.append('$').toString());
    }
}
