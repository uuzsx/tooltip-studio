package dev.tooltipstudio.config;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compatibility fingerprints only: no retired artwork or style definitions are distributed. */
public final class LegacyPresets {
    private static final Gson GSON = new Gson();
    private static final Map<String, String> FINGERPRINTS = Map.of(
            "cat_bell", "20ca23bfecfa8369b4d83b9efe3de7dfb1696680ff17c8ad6203d72811a3e955",
            "dream_dyeing_crystal_fragment", "3d41adf02567e2f9e5ebfbc13c8f2257e44404d9cde040c06acc7a5e7573bf8a",
            "epic", "18b1ed51ceac1b8340a625eb06f83fcc9b48026482e47396f00dd3d2d5e634f5",
            "legendary", "94be89f56a7d986155c2e2ae85654dfa0bfb1460053ead037156540ee00e2189",
            "mythical", "92fc8cd7f65ba53335df2650e24f305c68ab27b19b119ac47df3fdc3825d43ba",
            "rare", "bd173e2b454f24c57d23f9504655a60c2d5cff6c894b9c8a33d6d5a1db9c2305",
            "red_book", "ef8cf934ab8e111ff339d5f8284fb8b11d794907725f8d94f8a1e1d2b5c2d24b",
            "tslat_sword", "ee90f54494261cfba4cebd6460d5f0f70d501d7f834098fa8519080d7eddfb6b",
            "uncommon", "818d7cebb7a6ecfa0a0c2ffc5aa0ce13d4fabb8cd3456b574724a71a017b3ee1");

    private LegacyPresets() {}

    public static boolean unchanged(String id, Style style) {
        String expected = FINGERPRINTS.get(id);
        if (expected == null) return false;
        try {
            byte[] json = GSON.toJson(style).getBytes(StandardCharsets.UTF_8);
            return expected.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public static String resolve(String id, Set<String> available) {
        return id != null && !available.contains(id) && FINGERPRINTS.containsKey(id) ? "default" : id;
    }

    public static List<Settings.Rule> rules(List<Settings.Rule> rules, Set<String> available) {
        if (rules == null) return null;
        return rules.stream().map(rule -> rule == null ? null : new Settings.Rule(resolve(rule.style(), available),
                rule.priority(), rule.items(), rule.tags(), rule.rarities(), rule.nbt())).toList();
    }

    public static Settings settings(Settings source, Set<String> available) {
        return new Settings(source.schemaVersion(), source.enabled(), resolve(source.defaultStyle(), available),
                source.nbtStyleKey(), rules(source.rules(), available));
    }
}
