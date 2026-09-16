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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Actual framebuffer checks for left/right offsets, separator tracking, text, scale and pack reload. */
final class AdvancedDecorationSmoke extends Screen {
    private static final String PACK = "file/tooltip-studio-advanced-smoke.zip";
    private static final String[] ANCHORS = {"SEPARATOR_LEFT", "SEPARATOR_CENTER", "SEPARATOR_RIGHT"};
    private static final String[] COLORS = {"#FF5050", "#50FF50", "#5080FF"};
    private final MinecraftClient minecraft;
    private final Runnable after;
    private final Path root, config, baseFile, wideFile;
    private final byte[] originalConfig;
    private final List<String> originalPacks;
    private final List<Path> temporary = new ArrayList<>(), markers = new ArrayList<>();
    private final JsonObject base = SmokeClient.baseDefinition();
    private final AtomicInteger saved = new AtomicInteger();
    private final ItemStack normal = item("Separator demo", true), wrapped = item("A long item name that wraps across multiple title lines", true);
    private final ItemStack titleOnly = item("只有名称", false), tall = item("Tall text decoration", true), wide = item("样式内文本也可以缩放", true);
    private SmokeClient.SlotHarness slots;
    private NativeImage baseline;
    private PixelBox[] normalMarkers;
    private int frames, phase;
    private record PixelBox(int x, int y, int width, int height) {}

    private AdvancedDecorationSmoke(MinecraftClient client, Runnable after) throws Exception {
        super(Text.literal("Advanced tooltip decoration test"));
        minecraft = client; this.after = after;
        root = client.runDirectory.toPath().toAbsolutePath().normalize();
        config = root.resolve("config/tooltipstudio/config.json"); originalConfig = Files.readAllBytes(config);
        originalPacks = List.copyOf(client.getResourcePackManager().getEnabledNames());
        baseFile = config.getParent().resolve("styles/smoke_advanced/base.json");
        wideFile = config.getParent().resolve("styles/smoke_advanced/wide.json");
    }

    static void start(MinecraftClient client, Runnable after) {
        AdvancedDecorationSmoke screen = null;
        try { screen = new AdvancedDecorationSmoke(client, after); screen.prepare(); client.setScreen(screen); }
        catch (Exception | AssertionError e) {
            if (screen != null) screen.fail(e); else { e.printStackTrace(); client.scheduleStop(); }
            throw new IllegalStateException("ADVANCED SMOKE FAILED", e);
        }
    }

    private void prepare() throws Exception {
        base.addProperty("minWidth", 180); base.addProperty("maxWidth", 180);
        base.getAsJsonObject("separator").addProperty("inset", 5);
        writeNew(baseFile, base.toString());
        var wideStyle = base.deepCopy(); wideStyle.addProperty("minWidth", 230); wideStyle.addProperty("maxWidth", 230);
        wideStyle.getAsJsonArray("decorations").add(JsonParser.parseString("""
                {"type":"text","text":"STYLE TEXT","color":"#FFC078","anchor":"TOP_RIGHT","x":-4,"y":-14,
                 "foreground":true,"x_scale":1.25,"y_scale":0.75}
                """));
        writeNew(wideFile, wideStyle.toString()); wide.getOrCreateNbt().putString("TooltipStyle", "smoke_advanced/wide");
        for (int i = 0; i < 3; i++) {
            var d = new JsonObject(); d.addProperty("type", "text"); d.addProperty("text", "|"); d.addProperty("color", COLORS[i]);
            d.addProperty("anchor", ANCHORS[i]); d.addProperty("shadow", false); d.addProperty("foreground", true);
            Path path = config.getParent().resolve("decorations/smoke_advanced/marker_" + i + ".json");
            writeNew(path, d.toString()); markers.add(path);
        }
        NbtList lore = new NbtList();
        for (int i = 0; i < 60; i++) lore.add(NbtString.of(Text.Serialization.toJsonString(Text.literal("Scaled text and image line " + i))));
        tall.getOrCreateSubNbt("display").put("Lore", lore);
        reload();
        check(TooltipStudioClient.CONFIG.decorationCount() == 3, "text-only local definitions load with no PNG files");
    }

    private void writeNew(Path file, String text) throws Exception {
        check(!Files.exists(file), "new advanced test file: " + file.getFileName());
        Files.createDirectories(file.getParent()); Files.writeString(file, text); temporary.add(file);
    }
    private static ItemStack item(String name, boolean body) {
        var stack = new ItemStack(Items.STICK);
        stack.setCustomName(Text.literal(name).styled(s -> s.withItalic(false)));
        stack.getOrCreateNbt().putInt("HideFlags", 127); stack.getOrCreateNbt().putBoolean("DemoMarkers", true);
        stack.getOrCreateNbt().putString("TooltipStyle", "smoke_advanced/base");
        if (body) {
            NbtList lore = new NbtList();
            lore.add(NbtString.of(Text.Serialization.toJsonString(Text.literal("背景与正文仍使用原样式").formatted(Formatting.GRAY).styled(s -> s.withItalic(false)))));
            stack.getOrCreateSubNbt("display").put("Lore", lore);
        }
        return stack;
    }
    private void reload() { check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "advanced JSON reload"); }
    private void writeBase() throws Exception { Files.writeString(baseFile, base.toString()); reload(); }
    private void offset(int x, int y, String mode) throws Exception {
        base.addProperty("offsetX", x); base.addProperty("offsetY", y); base.addProperty("offsetXMode", mode); writeBase();
    }
    @Override protected void init() { slots = new SmokeClient.SlotHarness(); slots.init(minecraft, width, height); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try {
            context.fill(0, 0, width, height, 0xff253139);
            if (phase == 3 || phase == 6) return;
            if (phase == 0) {
                slots.show(context, normal, frames <= 30 ? 80 : width - 70, 140); context.draw();
                if (frames == 15) { capture(); offset(24, 0, "cursor"); }
                if (frames == 30) { verifyShift(24); offset(0, 0, "cursor"); }
                if (frames == 45) { capture(); offset(24, 0, "cursor"); }
                if (frames == 60) { verifyShift(-24); offset(-6, 0, "cursor"); }
                if (frames == 75) { verifyShift(6); offset(24, 0, "screen"); }
                if (frames == 90) {
                    verifyShift(24); offset(0, 0, "cursor");
                    var settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
                    settings.add("decorationRules", JsonParser.parseString("""
                            [{"decorations":["smoke_advanced/marker_0","smoke_advanced/marker_1","smoke_advanced/marker_2"],"nbt":{"DemoMarkers":true}}]
                            """));
                    Files.writeString(config, settings.toString()); reload(); phase = 1; frames = 0;
                }
            } else if (phase == 1) {
                slots.show(context, frames <= 15 || frames > 45 ? normal : frames <= 30 ? wrapped : titleOnly, 80, 140); context.draw();
                if (frames == 15) {
                    normalMarkers = markerBoxes(); verifyAnchors(normalMarkers, 1);
                    check(TooltipStudioClient.CONFIG.select(normal).decorations().stream().allMatch(d -> d.texture() == null),
                            "text decoration snapshots allocate no atlas texture");
                }
                if (frames == 30) {
                    var boxes = markerBoxes(); int rows = textRenderer.wrapLines(wrapped.getName(), 180).size();
                    int shift = (rows - 1) * 10 * minecraft.getWindow().getFramebufferHeight() / height;
                    check(rows > 1, "long title wraps for separator tracking test");
                    for (int i = 0; i < 3; i++) check(boxes[i].y() - normalMarkers[i].y() == shift, "separator anchor follows wrapped-title height");
                }
                if (frames == 45) { verifyHidden(); base.getAsJsonObject("separator").addProperty("enabled", false); writeBase(); }
                if (frames == 60) {
                    verifyHidden(); base.getAsJsonObject("separator").addProperty("enabled", true); writeBase();
                    for (Path path : markers) {
                        var d = JsonParser.parseString(Files.readString(path)).getAsJsonObject(); d.addProperty("x_scale", 2); d.addProperty("y_scale", 0.5);
                        Files.writeString(path, d.toString());
                    }
                    reload();
                }
                if (frames == 75) {
                    var scaled = markerBoxes(); verifyAnchors(scaled, 2);
                    for (int i = 0; i < 3; i++) check(Math.abs(scaled[i].width() - normalMarkers[i].width() * 2) <= 1
                            && scaled[i].height() < normalMarkers[i].height(), "text scales independently along X and Y in actual framebuffer");
                    String good = Files.readString(markers.get(0));
                    Files.writeString(markers.get(0), good.replace("\"x_scale\":2", "\"x_scale\":0"));
                    check(!TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "zero scale rejects reload and keeps the last snapshot");
                    Files.writeString(markers.get(0), good); reload();
                    base.getAsJsonArray("decorations").add(JsonParser.parseString("""
                            {"type":"text","text":"STYLE","color":"#12EF34","anchor":"BOTTOM","y":15,"foreground":true,"x_scale":1.25,"y_scale":0.75}
                            """));
                    writeBase(); phase = 2; frames = 0;
                }
            } else if (phase == 2) {
                slots.show(context, frames <= 60 ? normal : tall, 80, 140); context.draw();
                if (frames == 15) {
                    try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
                        check(colorBox(pixels, "#12EF34") != null, "text decorations embedded in a base style are visibly rendered");
                    }
                    offset(4096, 4096, "cursor");
                }
                if (frames == 30) { verifyBounds(); offset(-4096, -4096, "cursor"); }
                if (frames == 60) { verifyBounds(); offset(0, 0, "cursor"); }
                if (frames == 75) { verifyBounds(); phase = 3; loadExample(); }
            } else if (phase == 4) {
                context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.6 / 分割线锚点 · 文字装饰 · XY 缩放", width / 2, 20, 0xffe7dfce);
                context.drawCenteredTextWithShadow(textRenderer, "左右剑：x_scale 1.5 / y_scale 0.75；底部文字：0.8 倍", width / 2, 42, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "分割线：左侧图片 / 居中文字 / 右侧图片", 24, 84, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "标题换行后，装饰跟随分割线", 334, 84, 0xffb8c6cb);
                slots.show(context, normal, 24, 124); slots.show(context, wrapped, 334, 124);
                context.drawTextWithShadow(textRenderer, "没有分割线：仅保留底部文字", 24, 252, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "样式自带文字 + 独立装饰", 334, 252, 0xffb8c6cb);
                slots.show(context, titleOnly, 24, 310); slots.show(context, wide, 334, 310); context.draw();
                if (frames == 30) save("tooltip-studio-advanced-decorations.png");
                if (saved.get() == 1) { phase = 5; frames = 0; offset(24, 0, "cursor"); }
            } else if (phase == 5) {
                context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.6 / 左右两侧与鼠标保持相同间距", width / 2, 25, 0xffe7dfce);
                context.drawCenteredTextWithShadow(textRenderer, "offsetX: 24 / offsetXMode: cursor / 正值远离，负值靠近", width / 2, 48, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "tooltip 在鼠标右侧", 60, 102, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "tooltip 在鼠标左侧", 378, 102, 0xffb8c6cb);
                for (int cursor : new int[]{40, width - 40}) {
                    context.fill(cursor, 125, cursor + 1, 290, 0xfff0929b);
                    context.fill(cursor - 3, 177, cursor + 4, 178, 0xfff0929b);
                    context.drawTextWithShadow(textRenderer, "鼠标", cursor - 10, 302, 0xfff0929b);
                    slots.show(context, normal, cursor, 178);
                }
                context.draw();
                if (frames == 30) save("tooltip-studio-offset-sides.png");
                if (saved.get() == 2) {
                    phase = 6; cleanup(); reloadPacks(false).thenRun(() -> {
                        check(TooltipStudioClient.CONFIG.decorationCount() == 0, "advanced pack disable restores original decorations");
                        after.run();
                    }).exceptionally(e -> { fail(e); return null; });
                }
            }
            if (++frames > 1500) throw new IllegalStateException("Advanced screenshot timeout");
        } catch (Exception | AssertionError e) { fail(e); }
    }

    private void capture() { if (baseline != null) baseline.close(); baseline = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer()); }
    private void verifyShift(int offset) {
        try (var now = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            int dx = offset * now.getWidth() / width, background = baseline.getColor(0, 0), count = 0;
            for (int y = 0; y < now.getHeight(); y++) for (int x = 0; x < now.getWidth(); x++) {
                int sx = x - dx, expected = sx < 0 || sx >= now.getWidth() ? background : baseline.getColor(sx, y);
                if (expected != background) count++;
                if (now.getColor(x, y) != expected) throw new AssertionError("Side offset mismatch at " + x + "," + y);
            }
            check(count > 100, "cursor/screen horizontal offset moves exactly " + offset + " GUI pixels; zero framebuffer differences");
        }
    }
    private PixelBox[] markerBoxes() {
        try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            PixelBox[] boxes = new PixelBox[3];
            for (int i = 0; i < 3; i++) { boxes[i] = colorBox(pixels, COLORS[i]); check(boxes[i] != null, "separator text marker is visible: " + ANCHORS[i]); }
            return boxes;
        }
    }
    private PixelBox colorBox(NativeImage pixels, String color) {
        int rgb = Integer.parseInt(color.substring(1), 16), abgr = (rgb >> 16 & 255) | (rgb & 0xff00) | (rgb & 255) << 16;
        int minX = pixels.getWidth(), minY = pixels.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++) if ((pixels.getColor(x, y) & 0xffffff) == abgr) {
            minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        return maxX < 0 ? null : new PixelBox(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
    private void verifyAnchors(PixelBox[] boxes, int xScale) {
        int pixelScale = minecraft.getWindow().getFramebufferWidth() / width;
        int distance = (170 - textRenderer.getWidth("|") * xScale) * pixelScale;
        check(boxes[2].x() - boxes[0].x() == distance && Math.abs(boxes[1].x() * 2 - boxes[0].x() - boxes[2].x()) <= 1,
                "left/center/right text anchors use actual separator ends including inset and scaled text width");
        check(boxes[0].y() == boxes[1].y() && boxes[1].y() == boxes[2].y(), "separator text anchors share the same vertical center");
    }
    private void verifyHidden() {
        try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            for (String color : COLORS) check(colorBox(pixels, color) == null, "separator decoration is hidden when no separator is drawn");
        }
    }
    private void verifyBounds() {
        try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            int background = pixels.getColor(0, 0), mx = 4 * pixels.getWidth() / width, my = 4 * pixels.getHeight() / height;
            for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++) if (pixels.getColor(x, y) != background)
                if (x < mx || x >= pixels.getWidth() - mx || y < my || y >= pixels.getHeight() - my) throw new AssertionError("Scaled text escaped screen bounds");
            check(true, "scaled text and separator overlays remain inside screen bounds, including tall tooltip scaling");
        }
    }
    private void loadExample() throws Exception {
        Files.write(config, originalConfig); for (Path marker : markers) Files.delete(marker);
        base.getAsJsonArray("decorations").remove(0); offset(0, 0, "cursor");
        Path example = root.getParent().resolve("examples/advanced-decoration-pack"), zipPath = root.resolve("resourcepacks/tooltip-studio-advanced-smoke.zip");
        Files.createDirectories(zipPath.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(zipPath)); var paths = Files.walk(example)) {
            for (Path p : paths.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(example.relativize(p).toString().replace('\\', '/'))); Files.copy(p, zip); zip.closeEntry();
            }
        }
        for (var item : List.of(normal, wrapped, titleOnly, wide)) item.getOrCreateNbt().putString("TooltipDecorations", "advanced");
        reloadPacks(true).thenRun(() -> {
            var overlays = TooltipStudioClient.CONFIG.select(normal).decorations();
            check(overlays.size() == 4 && overlays.stream().filter(d -> d.definition().isText() && d.texture() == null).count() == 2,
                    "ZIP provides two PNG overlays and two text-only overlays without style replacement");
            check(overlays.stream().filter(d -> !d.definition().isText()).allMatch(d -> d.definition().scaleX() == 1.5f && d.definition().scaleY() == 0.75f),
                    "resource-pack PNG overlays retain independent X/Y scales");
            try { check(Arrays.equals(originalConfig, Files.readAllBytes(config)), "advanced pack leaves local config unchanged"); }
            catch (Exception e) { throw new IllegalStateException(e); }
            phase = 4; frames = 0; minecraft.setScreen(this);
        }).exceptionally(e -> { fail(e); return null; });
    }
    private CompletableFuture<Void> reloadPacks(boolean enabled) {
        var manager = minecraft.getResourcePackManager(); manager.scanPacks(); var names = new ArrayList<>(originalPacks);
        if (enabled) names.add(PACK); manager.setEnabledProfiles(names); return minecraft.reloadResources();
    }
    private void save(String name) { ScreenshotRecorder.saveScreenshot(minecraft.runDirectory, name, minecraft.getFramebuffer(), ignored -> saved.incrementAndGet()); }
    private void cleanup() throws Exception {
        if (baseline != null) { baseline.close(); baseline = null; }
        Files.write(config, originalConfig); for (Path p : temporary) Files.deleteIfExists(p);
    }
    private void fail(Throwable e) {
        phase = 6; System.err.println("ADVANCED SMOKE FAILED"); e.printStackTrace();
        try { cleanup(); } catch (Exception error) { error.printStackTrace(); }
        reloadPacks(false).whenComplete((ignored, error) -> minecraft.scheduleStop());
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); System.out.println("SMOKE PASS: " + message); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
