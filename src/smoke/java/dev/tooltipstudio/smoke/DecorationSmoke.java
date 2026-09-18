package dev.tooltipstudio.smoke;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tooltipstudio.TooltipStudioClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Local PNG and real ZIP overlays, pixel comparisons, pack overrides and reversible test configuration. */
final class DecorationSmoke extends Screen {
    private static final String PACK = "file/tooltip-studio-decorations-smoke.zip";
    private static final String HIGH = "file/tooltip-studio-decorations-high";
    private static final String[] CORNERS = {"top_left", "top_right", "bottom_left", "bottom_right"};
    private final MinecraftClient minecraft;
    private final Runnable after;
    private final Path root, config, local, alternate, highDefinition;
    private final byte[] originalConfig;
    private final List<String> originalPacks;
    private final List<Path> temporary = new ArrayList<>();
    private final JsonObject alternateJson = SmokeClient.baseDefinition();
    private final AtomicBoolean saved = new AtomicBoolean();
    private final ItemStack plain = item(false, false), matched = item(true, false), four = item(false, true);
    private final ItemStack other = item(false, true), tall = item(false, true);
    private SmokeClient.SlotHarness slots;
    private NativeImage baseline, decoratedBaseline;
    private int frames, phase;

    private DecorationSmoke(MinecraftClient client, Runnable after) throws Exception {
        super(Text.literal("Independent decoration verification"));
        minecraft = client;
        this.after = after;
        root = client.runDirectory.toPath().toAbsolutePath().normalize();
        config = root.resolve("config/tooltipstudio/config.json");
        local = config.getParent();
        originalConfig = Files.readAllBytes(config);
        originalPacks = List.copyOf(client.getResourcePackManager().getEnabledNames());
        alternate = local.resolve("styles/smoke_decorations/alternate.json");
        highDefinition = root.resolve("resourcepacks/tooltip-studio-decorations-high/assets/tooltipstudio/decorations/sword/top_left.json");
    }

    static void start(MinecraftClient client, Runnable after) {
        DecorationSmoke screen = null;
        try {
            screen = new DecorationSmoke(client, after);
            screen.prepare(); screen.verifyLocal(); client.setScreen(screen);
        } catch (Exception | AssertionError e) {
            if (screen != null) screen.fail(e);
            else { e.printStackTrace(); client.scheduleStop(); }
            throw new IllegalStateException("DECORATION SMOKE FAILED", e);
        }
    }

    private void prepare() throws Exception {
        Path example = root.getParent().resolve("examples/decoration-pack");
        Path packFile = root.resolve("resourcepacks/tooltip-studio-decorations-smoke.zip");
        Files.createDirectories(packFile.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(packFile)); var paths = Files.walk(example)) {
            for (Path p : paths.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(example.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, zip); zip.closeEntry();
            }
        }
        Path source = root.getParent().resolve("examples/independent-decorations");
        for (String corner : CORNERS) {
            Path target = local.resolve("decorations/sword/" + corner + ".json");
            check(!Files.exists(target), "smoke does not overwrite an existing decoration " + corner);
            writeTemporary(target, Files.readString(source.resolve("decorations/sword/" + corner + ".json")));
        }
        Path png = local.resolve("textures/decorations/sword.png");
        check(!Files.exists(png), "smoke does not overwrite an existing local sword PNG");
        Files.createDirectories(png.getParent());
        Files.copy(source.resolve("textures/decorations/sword.png"), png);
        temporary.add(png);
        var topLeft = JsonParser.parseString(Files.readString(local.resolve("decorations/sword/top_left.json"))).getAsJsonObject();
        topLeft.addProperty("x", 0); topLeft.addProperty("y", 0);
        Files.writeString(local.resolve("decorations/sword/top_left.json"), topLeft.toString());
        alternateJson.addProperty("minWidth", 180);
        alternateJson.getAsJsonArray("decorations").add(JsonParser.parseString("""
                {"region":{"u":0,"v":17,"width":16,"height":2},"anchor":"BOTTOM","x":0,"y":4,"foreground":true}
                """));
        writeTemporary(alternate, alternateJson.toString());
        other.getOrCreateNbt().putString("TooltipStyle", "smoke_decorations/alternate");
        tall.setNbt(other.getNbt().copy());
        NbtList lore = new NbtList();
        for (int i = 0; i < 60; i++) lore.add(NbtString.of(Text.Serialization.toJsonString(Text.literal("Tall overlay line " + i))));
        tall.getOrCreateSubNbt("display").put("Lore", lore);
        var settings = JsonParser.parseString(new String(originalConfig, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        var rules = JsonParser.parseString(Files.readString(source.resolve("decoration-rules.json"))).getAsJsonObject().getAsJsonArray("decorationRules");
        rules.add(JsonParser.parseString("""
                {"decorations":["sword/bottom_right"],"priority":500,"items":["minecraft:stick"],"tags":["tooltipstudio:smoke_overlay"],
                 "rarities":["common"],"nbt":{"OverlayCase":"combo"}}
                """));
        settings.add("decorationRules", rules);
        Files.writeString(config, settings.toString());
        reloadLocal();
    }

    private void writeTemporary(Path path, String text) throws Exception {
        check(!Files.exists(path), "test file is new: " + path.getFileName());
        Files.createDirectories(path.getParent()); Files.writeString(path, text); temporary.add(path);
    }

    private void verifyLocal() throws Exception {
        var manager = TooltipStudioClient.CONFIG;
        check(manager.decorationCount() == 4 && manager.select(plain).decorations().isEmpty(), "local decorations load without changing unmatched items");
        var before = matched.getNbt().copy();
        check(manager.select(matched).decorations().size() == 1 && manager.select(matched).style().equals(manager.select(plain).style()),
                "NBT adds only a decoration and preserves the exact base Style object values");
        check(before.equals(matched.getNbt()), "decoration matching is read-only");
        check(manager.select(new ItemStack(Items.SPRUCE_DOOR)).decorations().size() == 1, "decoration item ID condition");
        check(manager.select(four).decorations().size() == 4, "multiple corner decorations stack");
        var duplicate = four.copy(); duplicate.getOrCreateSubNbt("Monumenta").putString("Location", "forest");
        check(manager.select(duplicate).decorations().size() == 4, "repeated decoration IDs are drawn once across matching rules");
        check(manager.select(other).style().minWidth() == 180 && manager.select(other).style().decorations().size() == 1
                        && manager.select(other).decorations().size() == 4,
                "TooltipStyle-selected custom layout keeps its own decoration and receives independent overlays");
        var entry = Items.STICK.getRegistryEntry();
        var tags = entry.streamTags().toList();
        var bind = entry.getClass().getDeclaredMethod("setTags", java.util.Collection.class); bind.setAccessible(true);
        var combo = new ItemStack(Items.STICK); combo.getOrCreateNbt().putString("OverlayCase", "combo");
        try {
            check(manager.select(combo).decorations().isEmpty(), "missing tag skips a combined decoration rule");
            bind.invoke(entry, List.of(TagKey.of(RegistryKeys.ITEM, new Identifier("tooltipstudio", "smoke_overlay"))));
            check(manager.select(combo).decorations().size() == 1, "item ID AND tag AND rarity AND NBT decoration rule");
        } finally { bind.invoke(entry, tags); }
        Path definition = local.resolve("decorations/sword/top_left.json");
        String good = Files.readString(definition);
        Files.writeString(definition, good.replace("local:decorations/sword.png", "local:decorations/missing.png"));
        check(!manager.reload(minecraft.getResourceManager()) && manager.select(matched).decorations().size() == 1,
                "missing local decoration PNG retains complete previous snapshot");
        Files.writeString(definition, good); reloadLocal();
        byte[] savedConfig = Files.readAllBytes(config);
        try {
            var settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
            settings.add("decorationRules", JsonParser.parseString("""
                    [{"decorations":["sword/top_right","sword/bottom_left"],"priority":100,"items":["minecraft:stick"]},
                     {"decorations":["sword/top_left"],"priority":200,"items":["minecraft:stick"]},
                     {"decorations":["sword/bottom_right","sword/top_left"],"priority":100,"items":["minecraft:stick"]}]
                    """));
            Files.writeString(config, settings.toString()); reloadLocal();
            check(manager.select(plain).decorations().stream().map(d -> d.id()).toList().equals(
                            List.of("sword/bottom_right", "sword/bottom_left", "sword/top_right", "sword/top_left")),
                    "overlay draw order respects priority, stable ties and deduplication");
            settings.addProperty("enabled", false); Files.writeString(config, settings.toString()); reloadLocal();
            check(manager.select(plain) == null, "disabled mod also disables independent overlays");
        } finally { Files.write(config, savedConfig); reloadLocal(); }
    }

    private static ItemStack item(boolean forest, boolean all) {
        var stack = new ItemStack(Items.STICK);
        stack.setCustomName(Text.literal("独立装饰测试").styled(s -> s.withItalic(false)));
        stack.getOrCreateNbt().putInt("HideFlags", 127);
        NbtList lore = new NbtList();
        lore.add(NbtString.of(Text.Serialization.toJsonString(Text.literal("背景、边框、分割线仍用原样式")
                .formatted(net.minecraft.util.Formatting.GRAY).styled(s -> s.withItalic(false)))));
        stack.getOrCreateSubNbt("display").put("Lore", lore);
        if (forest) stack.getOrCreateSubNbt("Monumenta").putString("Location", "forest");
        if (all) stack.getOrCreateNbt().putString("TooltipDecorations", "all_corners");
        return stack;
    }

    private void reloadLocal() { check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "decoration configuration reload"); }
    private void offset(int x, int y) throws Exception {
        alternateJson.addProperty("offsetX", x); alternateJson.addProperty("offsetY", y);
        Files.writeString(alternate, alternateJson.toString()); reloadLocal();
    }

    @Override protected void init() { slots = new SmokeClient.SlotHarness(); slots.init(minecraft, width, height); }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try {
            context.fill(0, 0, width, height, 0xff253139);
            if (phase == 1 || phase == 3) return;
            if (phase == 0) {
                slots.show(context, frames <= 15 ? plain : frames <= 30 ? matched : frames <= 90 ? other : tall, 70, 140);
                context.draw();
                if (frames == 15) baseline = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer());
                if (frames == 30) verifyOnlyCornerChanged();
                if (frames == 45) { decoratedBaseline = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer()); offset(24, -20); }
                if (frames == 60) { verifyShift(); offset(4096, 4096); }
                if (frames == 75) { verifyBounds(); offset(-4096, -4096); }
                if (frames == 90) { verifyBounds(); offset(0, 0); }
                if (frames == 105) { verifyBounds(); phase = 1; testPacks(); }
            } else {
                context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.7 / 按条件叠加独立装饰", width / 2, 22, 0xffe7e0cf);
                context.drawCenteredTextWithShadow(textRenderer, "同一张剑贴图可用于四角，也可叠加到其他 tooltip 样式", width / 2, 44, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "未命中条件：保持原样", 24, 88, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "命中 forest：左上角加小剑", 330, 88, 0xffb8c6cb);
                slots.show(context, plain, 24, 132); slots.show(context, matched, 330, 132);
                context.drawTextWithShadow(textRenderer, "多项叠加：默认款的四个角", 24, 254, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "自定义布局：四角 + 样式自身装饰", 330, 254, 0xffb8c6cb);
                slots.show(context, four, 24, 308); slots.show(context, other, 330, 308);
                context.draw();
                if (frames == 30) ScreenshotRecorder.saveScreenshot(minecraft.runDirectory, "tooltip-studio-decorations.png",
                        minecraft.getFramebuffer(), ignored -> saved.set(true));
                if (saved.get()) {
                    phase = 3;
                    cleanupFiles();
                    reloadPacks().thenRun(() -> {
                        check(TooltipStudioClient.CONFIG.decorationCount() == 0 && TooltipStudioClient.CONFIG.select(matched).decorations().isEmpty(),
                                "disabling decoration pack removes overlays and retains original style");
                        after.run();
                    }).exceptionally(e -> { fail(e); return null; });
                }
            }
            if (++frames > 1200) throw new IllegalStateException("Decoration screenshot timed out");
        } catch (Exception | AssertionError e) { fail(e); }
    }

    private void verifyOnlyCornerChanged() {
        try (var now = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            int background = baseline.getColor(0, 0), minX = baseline.getWidth(), minY = baseline.getHeight();
            for (int y = 0; y < baseline.getHeight(); y++) for (int x = 0; x < baseline.getWidth(); x++)
                if (baseline.getColor(x, y) != background) { minX = Math.min(minX, x); minY = Math.min(minY, y); }
            int changed = 0, sizeX = 16 * now.getWidth() / width, sizeY = 16 * now.getHeight() / height;
            for (int y = 0; y < now.getHeight(); y++) for (int x = 0; x < now.getWidth(); x++) {
                if (now.getColor(x, y) == baseline.getColor(x, y)) continue;
                changed++;
                if (x < minX || x >= minX + sizeX || y < minY || y >= minY + sizeY)
                    throw new AssertionError("Independent overlay changed a pixel outside its corner: " + x + "," + y);
            }
            check(changed > 0, "pixel comparison: only the 16x16 corner changes; all other tooltip pixels stay identical");
        }
    }

    private void verifyShift() {
        try (var now = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            int dx = 24 * now.getWidth() / width, dy = -20 * now.getHeight() / height, background = decoratedBaseline.getColor(0, 0);
            for (int y = 0; y < now.getHeight(); y++) for (int x = 0; x < now.getWidth(); x++) {
                int sx = x - dx, sy = y - dy;
                int expected = sx < 0 || sy < 0 || sx >= now.getWidth() || sy >= now.getHeight() ? background : decoratedBaseline.getColor(sx, sy);
                if (now.getColor(x, y) != expected) throw new AssertionError("Decoration did not follow whole-tooltip offset");
            }
            check(true, "all four independent overlays and the base move together: zero pixel differences");
        }
    }

    private void verifyBounds() {
        try (var image = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            int background = image.getColor(0, 0), marginX = 4 * image.getWidth() / width, marginY = 4 * image.getHeight() / height, pixels = 0;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                if (image.getColor(x, y) == background) continue;
                pixels++;
                if (x < marginX || x >= image.getWidth() - marginX || y < marginY || y >= image.getHeight() - marginY)
                    throw new AssertionError("Independent decoration escaped the screen margin");
            }
            check(pixels > 100, "independent corner overlays remain inside screen bounds, including tall scaled tooltips");
        }
    }

    private void testPacks() throws Exception {
        Files.write(config, originalConfig);
        Files.writeString(local.resolve("decorations/sword/top_left.json"), "{broken-local-override");
        Files.createDirectories(highDefinition.getParent());
        Path high = root.resolve("resourcepacks/tooltip-studio-decorations-high");
        Files.writeString(high.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":22,\"description\":\"Decoration override test\"}}");
        var highJson = JsonParser.parseString(Files.readString(root.getParent().resolve("examples/decoration-pack/assets/tooltipstudio/decorations/sword/top_left.json"))).getAsJsonObject();
        highJson.addProperty("x", -8); Files.writeString(highDefinition, highJson.toString());
        Path highRules = high.resolve("assets/tooltipstudio/rules/independent_decorations.json");
        Files.createDirectories(highRules.getParent());
        Files.writeString(highRules, "{\"schemaVersion\":1,\"decorationRules\":[]}");
        reloadPacks(PACK).thenRun(() -> {
            check(TooltipStudioClient.CONFIG.decorationCount() == 4 && TooltipStudioClient.CONFIG.select(matched).decorations().get(0).definition().x() == -6,
                    "ZIP decorations override the matching local path without reading the shadowed invalid file");
            check(TooltipStudioClient.CONFIG.select(plain).style().equals(TooltipStudioClient.CONFIG.select(matched).style()),
                    "decoration-only ZIP does not switch base styles");
            try {
                var settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
                settings.add("decorationRules", JsonParser.parseString("""
                        [{"decorations":["sword/top_right"],"priority":200,"nbt":{"Monumenta.Location":"forest"}}]
                        """));
                Files.writeString(config, settings.toString()); reloadLocal();
                check(TooltipStudioClient.CONFIG.select(matched).decorations().stream().map(d -> d.id()).toList()
                                .equals(List.of("sword/top_left", "sword/top_right")),
                        "equal-priority local overlay paints above the resource-pack overlay");
                Files.write(config, originalConfig);
            } catch (Exception e) { throw new IllegalStateException(e); }
        }).thenCompose(ignored -> reloadPacks(PACK, HIGH)).thenRun(() -> {
            check(TooltipStudioClient.CONFIG.select(matched).decorations().isEmpty(), "higher pack empty decorationRules disables lower file");
            try {
                var settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
                settings.add("decorationRules", JsonParser.parseString("""
                        [{"decorations":["sword/top_left"],"priority":200,"nbt":{"Monumenta.Location":"forest"}}]
                        """));
                Files.writeString(config, settings.toString()); reloadLocal();
                check(TooltipStudioClient.CONFIG.select(matched).decorations().get(0).definition().x() == -8,
                        "higher pack overrides decoration placement for a local rule");
                Files.writeString(highDefinition, "{broken-pack-decoration");
            } catch (Exception e) { throw new IllegalStateException(e); }
        }).thenCompose(ignored -> reloadPacks(PACK, HIGH)).thenRun(() -> {
            check(TooltipStudioClient.CONFIG.lastError() != null && TooltipStudioClient.CONFIG.lastError().contains("decorations/sword/top_left.json")
                            && TooltipStudioClient.CONFIG.select(matched).decorations().get(0).definition().x() == -8,
                    "invalid pack decoration retains the last working base, overlays and rules");
            try { Files.write(config, originalConfig); } catch (Exception e) { throw new IllegalStateException(e); }
        }).thenCompose(ignored -> reloadPacks(PACK)).thenRun(() -> {
            try {
                check(Arrays.equals(originalConfig, Files.readAllBytes(config)), "decoration pack loading leaves local config unchanged");
                check(TooltipStudioClient.CONFIG.select(other).decorations().size() == 4, "pack overlays also apply to a custom style selected by TooltipStyle");
                phase = 2; frames = 0; minecraft.setScreen(this);
            } catch (Exception e) { throw new IllegalStateException(e); }
        }).exceptionally(e -> { fail(e); return null; });
    }

    private CompletableFuture<Void> reloadPacks(String... extra) {
        var manager = minecraft.getResourcePackManager(); manager.scanPacks();
        var names = new ArrayList<>(originalPacks); names.addAll(List.of(extra)); manager.setEnabledProfiles(names);
        return minecraft.reloadResources();
    }

    private void cleanupFiles() throws Exception {
        if (baseline != null) { baseline.close(); baseline = null; }
        if (decoratedBaseline != null) { decoratedBaseline.close(); decoratedBaseline = null; }
        Files.write(config, originalConfig);
        for (Path path : temporary) Files.deleteIfExists(path);
    }
    private void fail(Throwable error) {
        phase = 3; System.err.println("DECORATION SMOKE FAILED"); error.printStackTrace();
        try { cleanupFiles(); } catch (Exception e) { e.printStackTrace(); }
        reloadPacks().whenComplete((ignored, e) -> minecraft.scheduleStop());
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); System.out.println("SMOKE PASS: " + message); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
