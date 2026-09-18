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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Real glyph pixels verify contiguous runs, one shared anchor/scale, reload, and a text-only ZIP. */
final class SegmentedTextSmoke extends Screen {
    private static final String PACK = "file/tooltip-studio-segments-smoke.zip";
    private static final String[] COLORS = {"#FF5050", "#50FF50", "#5080FF"};
    private final MinecraftClient minecraft;
    private final Runnable after;
    private final Path run, config, styleFile, inlineFile, markerFile;
    private final byte[] originalConfig;
    private final List<String> originalPacks;
    private final List<Path> temporary = new ArrayList<>();
    private final JsonObject style = SmokeClient.baseDefinition();
    private JsonObject marker = JsonParser.parseString("""
            {"type":"text","text":"III","color":"#FF5050","anchor":"BOTTOM","y":16,"foreground":true,"shadow":false}
            """).getAsJsonObject();
    private final ItemStack sample = item("Rooted Walkers", "artifact"), wrapped = item("A longer name with exactly the same colored label", "artifact"),
            masterwork = item("Masterwork 示例", "masterwork"), inline = item("样式内也支持拼接文字", "none");
    private final AtomicBoolean saved = new AtomicBoolean();
    private SmokeClient.SlotHarness slots;
    private NativeImage baseline;
    private Box[] normal;
    private int frames, phase;
    private record Box(int x, int y, int width, int height) {}

    private SegmentedTextSmoke(MinecraftClient client, Runnable after) throws Exception {
        super(Text.literal("Segmented text decoration smoke"));
        minecraft = client; this.after = after;
        run = client.runDirectory.toPath().toAbsolutePath().normalize();
        config = run.resolve("config/tooltipstudio/config.json"); originalConfig = Files.readAllBytes(config);
        originalPacks = List.copyOf(client.getResourcePackManager().getEnabledNames());
        styleFile = config.getParent().resolve("styles/smoke_segments/base.json");
        inlineFile = config.getParent().resolve("styles/smoke_segments/inline.json");
        markerFile = config.getParent().resolve("decorations/smoke_segments/marker.json");
    }
    static void start(MinecraftClient client, Runnable after) {
        SegmentedTextSmoke screen = null;
        try { screen = new SegmentedTextSmoke(client, after); screen.prepare(); client.setScreen(screen); }
        catch (Exception | AssertionError e) {
            if (screen != null) screen.fail(e); else { e.printStackTrace(); client.scheduleStop(); }
            throw new IllegalStateException("SEGMENTS SMOKE FAILED", e);
        }
    }
    private void prepare() throws Exception {
        style.addProperty("minWidth", 220); style.addProperty("maxWidth", 220);
        writeNew(styleFile, style.toString());
        var embedded = style.deepCopy();
        embedded.getAsJsonArray("decorations").add(JsonParser.parseString("""
                {"type":"text","segments":[{"text":"同一个装饰 / ","color":"#50FF50"},
                 {"text":"不同的颜色","color":"#5080FF","italic":true}],
                 "anchor":"BOTTOM","y":18,"foreground":true,"x_scale":1.15,"y_scale":0.85}
                """));
        writeNew(inlineFile, embedded.toString()); inline.getOrCreateNbt().putString("TooltipStyle", "smoke_segments/inline");
        writeNew(markerFile, marker.toString());
        var settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
        settings.add("decorationRules", JsonParser.parseString("""
                [{"decorations":["smoke_segments/marker"],"nbt":{"TooltipTextDemo":"artifact"}}]
                """));
        Files.writeString(config, settings.toString()); reload();
    }
    private void writeNew(Path file, String contents) throws Exception {
        check(!Files.exists(file), "new segmented-text file " + file.getFileName());
        Files.createDirectories(file.getParent()); Files.writeString(file, contents); temporary.add(file);
    }
    private static ItemStack item(String name, String kind) {
        var stack = new ItemStack(Items.LEATHER_BOOTS);
        stack.setCustomName(Text.literal(name).styled(s -> s.withItalic(false)));
        stack.getOrCreateNbt().putInt("HideFlags", 127);
        stack.getOrCreateNbt().putString("TooltipStyle", "smoke_segments/base");
        stack.getOrCreateNbt().putString("TooltipTextDemo", kind);
        var lore = new NbtList();
        lore.add(NbtString.of(Text.Serialization.toJsonString(Text.literal("基础款背景、边框和正文保持原样").formatted(Formatting.GRAY).styled(s -> s.withItalic(false)))));
        stack.getOrCreateSubNbt("display").put("Lore", lore); return stack;
    }
    private void reload() { check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "segmented text JSON reload"); }
    private void writeMarker() throws Exception { Files.writeString(markerFile, marker.toString()); reload(); }
    private void offset(int value) throws Exception {
        style.addProperty("offsetX", value); style.addProperty("offsetY", value);
        Files.writeString(styleFile, style.toString()); reload();
    }
    @Override protected void init() { slots = new SmokeClient.SlotHarness(); slots.init(minecraft, width, height); }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try {
            context.fill(0, 0, width, height, 0xff253139);
            if (phase == 0) {
                slots.show(context, sample, 80, 140); context.draw();
                if (frames == 15) {
                    baseline = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer()); marker.remove("text");
                    marker.add("segments", JsonParser.parseString("[{\"text\":\"I\"},{\"text\":\"I\"},{\"text\":\"I\"}]")); writeMarker();
                }
                if (frames == 30) {
                    try (var now = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
                        for (int y = 0; y < now.getHeight(); y++) for (int x = 0; x < now.getWidth(); x++)
                            if (now.getColor(x, y) != baseline.getColor(x, y)) throw new AssertionError("String/segments pixel mismatch");
                    }
                    baseline.close(); baseline = null;
                    check(true, "plain string and same-color segments are pixel-identical");
                    for (int i = 0; i < 3; i++) marker.getAsJsonArray("segments").get(i).getAsJsonObject().addProperty("color", COLORS[i]);
                    writeMarker();
                }
                if (frames == 45) {
                    normal = boxes(); int advance = textRenderer.getWidth("I") * minecraft.getWindow().getFramebufferWidth() / width;
                    check(normal[1].x() - normal[0].x() == advance && normal[2].x() - normal[1].x() == advance,
                            "colored segments concatenate using exact glyph advances with no extra gaps");
                    check(normal[0].y() == normal[1].y() && normal[1].y() == normal[2].y(), "colored segments share one baseline");
                    marker.addProperty("x_scale", 2); marker.addProperty("y_scale", 0.5); writeMarker();
                }
                if (frames == 60) {
                    var scaled = boxes();
                    check(scaled[1].x() - scaled[0].x() == 2 * (normal[1].x() - normal[0].x())
                            && scaled[0].height() < normal[0].height(), "all segments share independent X/Y scaling");
                    check(Math.abs(scaled[0].x() + scaled[2].x() + scaled[2].width()
                            - normal[0].x() - normal[2].x() - normal[2].width()) <= 2, "segmented text keeps one centered anchor when scaled");
                    marker.addProperty("x_scale", 1); marker.addProperty("y_scale", 1);
                    marker.getAsJsonArray("segments").get(0).getAsJsonObject().addProperty("text", "I\n"); writeMarker();
                }
                if (frames == 75) {
                    var multiline = boxes(); int pixelScale = minecraft.getWindow().getFramebufferHeight() / height;
                    check(multiline[0].x() == multiline[1].x() && multiline[1].y() - multiline[0].y() == textRenderer.fontHeight * pixelScale,
                            "segment newline preserves colors and starts the next line at the common origin");
                    var previous = TooltipStudioClient.CONFIG.select(sample).decorations().get(0);
                    var invalid = marker.deepCopy(); invalid.getAsJsonArray("segments").get(1).getAsJsonObject().addProperty("color", "bad");
                    Files.writeString(markerFile, invalid.toString());
                    check(!TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                            && TooltipStudioClient.CONFIG.select(sample).decorations().get(0) == previous,
                            "invalid segment color retains the complete previous snapshot");
                    writeMarker(); offset(4096);
                }
                if (frames == 90) { verifyBounds(); offset(-4096); }
                if (frames == 105) { verifyBounds(); offset(0); phase = 1; loadPack(); }
            } else if (phase == 2) {
                context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.7 / 一个装饰 · 多段文字 · 独立颜色", width / 2, 20, 0xffe7dfce);
                context.drawCenteredTextWithShadow(textRenderer, "segments 按顺序拼接，共用位置、锚点、阴影与 XY 缩放", width / 2, 42, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "灰色前缀 + 红色 Artifact", 22, 84, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "名称变长，整段装饰仍自动居中", 334, 84, 0xffb8c6cb);
                slots.show(context, sample, 22, 125); slots.show(context, wrapped, 334, 125);
                context.drawTextWithShadow(textRenderer, "继承灰色 / 金色粗体 / 换行与斜体", 22, 252, 0xffb8c6cb);
                context.drawTextWithShadow(textRenderer, "样式内装饰，同样支持多色和缩放", 334, 252, 0xffb8c6cb);
                slots.show(context, masterwork, 22, 305); slots.show(context, inline, 334, 305); context.draw();
                if (frames == 15) {
                    try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
                        for (String color : new String[]{"#FF5555", "#FFD24A", "#50FF50", "#5080FF"})
                            check(colorBox(pixels, color) != null, "pack/embedded segmented text color is visible: " + color);
                    }
                }
                if (frames == 30) ScreenshotRecorder.saveScreenshot(minecraft.runDirectory, "tooltip-studio-segmented-text.png", minecraft.getFramebuffer(), ignored -> saved.set(true));
                if (saved.get()) {
                    phase = 3; cleanup(); reloadPacks(false).thenRun(() -> {
                        check(TooltipStudioClient.CONFIG.decorationCount() == 0, "disabling segmented pack restores original definitions"); after.run();
                    }).exceptionally(e -> { fail(e); return null; });
                }
            }
            if (++frames > 1500 && phase != 3) throw new IllegalStateException("Segmented text smoke timeout");
        } catch (Exception | AssertionError e) { fail(e); }
    }
    private Box[] boxes() {
        try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            var result = new Box[3];
            for (int i = 0; i < 3; i++) { result[i] = colorBox(pixels, COLORS[i]); check(result[i] != null, "segment color rendered " + COLORS[i]); }
            return result;
        }
    }
    private Box colorBox(NativeImage pixels, String color) {
        int rgb = Integer.parseInt(color.substring(1), 16), abgr = (rgb >> 16 & 255) | (rgb & 0xff00) | (rgb & 255) << 16;
        int left = pixels.getWidth(), top = pixels.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++) if ((pixels.getColor(x, y) & 0xffffff) == abgr) {
            left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x); bottom = Math.max(bottom, y);
        }
        return right < 0 ? null : new Box(left, top, right - left + 1, bottom - top + 1);
    }
    private void verifyBounds() {
        try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            int bg = pixels.getColor(0, 0), mx = 4 * pixels.getWidth() / width, my = 4 * pixels.getHeight() / height;
            for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++) if (pixels.getColor(x, y) != bg)
                if (x < mx || x >= pixels.getWidth() - mx || y < my || y >= pixels.getHeight() - my) throw new AssertionError("Segmented text escaped screen bounds");
            check(true, "multiline segmented text stays inside screen edges");
        }
    }
    private void loadPack() throws Exception {
        Files.write(config, originalConfig); Files.delete(markerFile);
        Path example = run.getParent().resolve("examples/segmented-text-pack"), target = run.resolve("resourcepacks/tooltip-studio-segments-smoke.zip");
        Files.createDirectories(target.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(target)); var paths = Files.walk(example)) {
            for (var p : paths.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(example.relativize(p).toString().replace('\\', '/'))); Files.copy(p, zip); zip.closeEntry();
            }
        }
        reloadPacks(true).thenRun(() -> {
            check(TooltipStudioClient.CONFIG.decorationCount() == 2, "text-only ZIP loads two segmented definitions");
            var selected = TooltipStudioClient.CONFIG.select(sample).decorations();
            check(selected.size() == 1 && selected.get(0).definition().segments().size() == 2 && selected.get(0).texture() == null,
                    "NBT rule selects one multicolor decoration without any PNG");
            try { check(Arrays.equals(originalConfig, Files.readAllBytes(config)), "segmented pack leaves local config unchanged"); }
            catch (Exception e) { throw new IllegalStateException(e); }
            phase = 2; frames = 0; minecraft.setScreen(this);
        }).exceptionally(e -> { fail(e); return null; });
    }
    private CompletableFuture<Void> reloadPacks(boolean enabled) {
        var manager = minecraft.getResourcePackManager(); manager.scanPacks(); var names = new ArrayList<>(originalPacks);
        if (enabled) names.add(PACK); manager.setEnabledProfiles(names); return minecraft.reloadResources();
    }
    private void cleanup() throws Exception {
        if (baseline != null) { baseline.close(); baseline = null; }
        Files.write(config, originalConfig); for (var p : temporary) Files.deleteIfExists(p);
    }
    private void fail(Throwable error) {
        phase = 3; System.err.println("SEGMENTS SMOKE FAILED"); error.printStackTrace();
        try { cleanup(); } catch (Exception e) { e.printStackTrace(); }
        reloadPacks(false).whenComplete((ignored, e) -> minecraft.scheduleStop());
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); System.out.println("SMOKE PASS: " + message); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
