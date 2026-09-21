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
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Real timed UV changes, stable layout, separate local atlases, failed reloads and the shipped ZIP. */
final class AnimatedDecorationSmoke extends Screen {
    private static final String PACK = "file/tooltip-studio-animation-smoke.zip";
    // NativeImage colors are ABGR. Each group identifies one decoration independently of its frame.
    private static final int[] LEFT = {0xff3545ed, 0xff49db36, 0xffed6935};
    private static final int[] RIGHT = {0xffc346c7, 0xff39d9ed, 0xffded332};
    private static final int STATIC = 0xff277ecb;
    private final MinecraftClient minecraft;
    private final Runnable after;
    private final Path run, config, styleFile, markerFile;
    private final byte[] originalConfig;
    private final List<String> originalPacks;
    private final List<Path> temporary = new ArrayList<>();
    private final JsonObject style = SmokeClient.baseDefinition();
    private final Set<Integer> leftFrames = new HashSet<>(), rightFrames = new HashSet<>();
    private final ItemStack sample = new ItemStack(Items.STICK);
    private SmokeClient.SlotHarness slots;
    private NativeImage baseline;
    private Box[] boxes;
    private int phase, frames, captures;
    private long phaseStart, nextCapture;
    private record Box(int x, int y, int w, int h, int color) {
        boolean contains(int px, int py) { return px >= x && py >= y && px < x + w && py < y + h; }
    }

    private AnimatedDecorationSmoke(MinecraftClient client, Runnable after) throws Exception {
        super(Text.literal("Animated decorations")); minecraft = client; this.after = after;
        run = client.runDirectory.toPath().toAbsolutePath().normalize();
        config = run.resolve("config/tooltipstudio/config.json"); originalConfig = Files.readAllBytes(config);
        originalPacks = List.copyOf(client.getResourcePackManager().getEnabledNames());
        styleFile = config.getParent().resolve("styles/smoke_animation/base.json");
        markerFile = config.getParent().resolve("decorations/smoke_animation/right.json");
        sample.getOrCreateNbt().putString("TooltipStyle", "smoke_animation/base");
    }
    static void start(MinecraftClient client, Runnable after) {
        AnimatedDecorationSmoke screen = null;
        try {
            screen = new AnimatedDecorationSmoke(client, after);
            screen.writeNew(screen.styleFile, screen.style.toString()); screen.reload(); client.setScreen(screen);
        } catch (Exception | AssertionError e) {
            if (screen != null) screen.fail(e); else { e.printStackTrace(); client.scheduleStop(); }
            throw new IllegalStateException("ANIMATION SMOKE FAILED", e);
        }
    }
    private void writeNew(Path p, String contents) throws Exception {
        check(!Files.exists(p), "animation test file is new: " + p.getFileName());
        Files.createDirectories(p.getParent()); Files.writeString(p, contents); temporary.add(p);
    }
    private void png(String name, int w, int h, int[] colors, boolean horizontal) throws Exception {
        Path p = config.getParent().resolve("textures/" + name);
        check(!Files.exists(p), "animation test texture is new"); temporary.add(p);
        try (var image = new NativeImage(w, h, true)) {
            for (int f = 0; f < colors.length; f++) for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++)
                image.setColor(2 + x + (horizontal ? f * 8 : 0), 3 + y + (horizontal ? 0 : f * 8), colors[f]);
            image.writeTo(p);
        }
    }
    private void installDecorations() throws Exception {
        png("smoke_animation_vertical.png", 12, 27, LEFT, false);
        png("smoke_animation_horizontal.png", 28, 12, RIGHT, true);
        png("smoke_animation_static.png", 12, 12, new int[]{STATIC}, false);
        style.getAsJsonArray("decorations").add(JsonParser.parseString("""
            {"texture":"local:smoke_animation_vertical.png","textureWidth":12,"textureHeight":27,
             "region":{"u":2,"v":3,"width":8,"height":8},"animation":{"frames":3,"frameTime":2},
             "anchor":"TOP_LEFT","foreground":true,"x_scale":1.5,"y_scale":0.75}
            """));
        style.getAsJsonArray("decorations").add(JsonParser.parseString("""
            {"texture":"local:smoke_animation_static.png","textureWidth":12,"textureHeight":12,
             "region":{"u":2,"v":3,"width":5,"height":7},"anchor":"BOTTOM_LEFT","foreground":true}
            """));
        Files.writeString(styleFile, style.toString());
        writeNew(markerFile, """
            {"texture":"local:smoke_animation_horizontal.png","textureWidth":28,"textureHeight":12,
             "region":{"u":2,"v":3,"width":8,"height":8},"animation":{"frames":3,"frameTime":3,"direction":"horizontal"},
             "anchor":"TOP_RIGHT","foreground":true}
            """);
        var settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
        settings.add("decorationRules", JsonParser.parseString("""
            [{"decorations":["smoke_animation/right"],"items":["minecraft:stick"]}]
            """));
        Files.writeString(config, settings.toString()); reload();
        var selected = TooltipStudioClient.CONFIG.select(sample);
        check(selected.inlineTextures().size() == 2 && selected.decorations().size() == 1,
                "inline private textures survive independently matched overlays");
        var invalid = style.deepCopy();
        invalid.getAsJsonArray("decorations").get(0).getAsJsonObject().addProperty("texture", "local:missing_animation.png");
        Files.writeString(styleFile, invalid.toString());
        check(!TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                        && TooltipStudioClient.CONFIG.select(sample).style() == selected.style(),
                "missing inline PNG retains the entire previous snapshot");
        invalid = style.deepCopy();
        invalid.getAsJsonArray("decorations").get(0).getAsJsonObject().getAsJsonObject("animation").addProperty("frames", 4);
        Files.writeString(styleFile, invalid.toString());
        check(!TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                        && TooltipStudioClient.CONFIG.select(sample).style() == selected.style(),
                "out-of-bounds animation retains the entire previous snapshot");
        Files.writeString(styleFile, style.toString()); reload();
    }
    private void reload() { check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "animation configuration reload"); }
    @Override protected void init() { slots = new SmokeClient.SlotHarness(); slots.init(minecraft, width, height); }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try {
            context.fill(0, 0, width, height, 0xff253139);
            if (phase <= 1) {
                slots.show(context, sample, 80, 140); context.draw();
                if (phase == 0 && frames == 15) {
                    baseline = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer());
                    installDecorations(); phase = 1; phaseStart = System.nanoTime();
                } else if (phase == 1) {
                    verifyPixels();
                    if (System.nanoTime() - phaseStart > 1_600_000_000L) {
                        check(leftFrames.size() == 3 && rightFrames.size() == 3, "all vertical and horizontal frames visibly loop in the real client");
                        check(true, "frame changes keep fixed scaled bounds; all pixels outside decorations match the undecorated tooltip");
                        phase = 2; loadPack();
                    }
                }
            } else if (phase == 3) {
                context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.8 / 动态装饰 · 单独贴图", width / 2, 45, 0xffe7dfce);
                context.drawCenteredTextWithShadow(textRenderer, "左上：样式内引用独立 PNG    右上：匹配后叠加", width / 2, 72, 0xffb8c6cb);
                slots.show(context, sample, 205, 175);
                context.drawCenteredTextWithShadow(textRenderer, "8 帧 PNG / 每帧 0.1 秒 / 默认款面板", width / 2, 310, 0xffb8c6cb);
                context.draw();
                long now = System.nanoTime();
                if (now >= nextCapture && captures < 8) {
                    try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
                        check(find(pixels, new int[]{0xffccc828}) != null, "example pack animated PNG is visible");
                        Path output = run.resolve("screenshots/tooltip-studio-animation-" + captures + ".png");
                        Files.createDirectories(output.getParent()); pixels.writeTo(output);
                    }
                    captures++; nextCapture = now + 100_000_000L;
                }
                if (captures == 8) {
                    phase = 4; cleanup(); reloadPacks(false).thenRun(() -> {
                        check(TooltipStudioClient.CONFIG.count() == 1 && TooltipStudioClient.CONFIG.decorationCount() == 0,
                                "disabling animation pack removes its style and decorations"); after.run();
                    }).exceptionally(e -> { fail(e); return null; });
                }
            }
            if (++frames > 2500 && phase != 4) throw new IllegalStateException("Animation smoke timeout");
        } catch (Exception | AssertionError e) { fail(e); }
    }
    private void verifyPixels() {
        try (var pixels = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            Box[] now = {find(pixels, LEFT), find(pixels, RIGHT), find(pixels, new int[]{STATIC})};
            for (Box b : now) if (b == null) throw new AssertionError("Missing animated/static decoration pixels");
            leftFrames.add(now[0].color); rightFrames.add(now[1].color);
            int scale = pixels.getWidth() / width;
            if (boxes == null) {
                boxes = now;
                check(now[0].w == 12 * scale && now[0].h == 6 * scale && now[1].w == 8 * scale && now[1].h == 8 * scale,
                        "animated decoration uses one frame and independent XY scaling");
                check(now[2].w == 5 * scale && now[2].h == 7 * scale, "static inline private PNG renders at its own dimensions");
            }
            for (int i = 0; i < 3; i++) if (now[i].x != boxes[i].x || now[i].y != boxes[i].y || now[i].w != boxes[i].w || now[i].h != boxes[i].h)
                throw new AssertionError("Animated decoration moved/resized between frames");
            for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++)
                if (!boxes[0].contains(x,y) && !boxes[1].contains(x,y) && !boxes[2].contains(x,y)
                        && pixels.getColor(x,y) != baseline.getColor(x,y)) throw new AssertionError("Animation changed base tooltip pixels");
        }
    }
    private static Box find(NativeImage image, int[] colors) {
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1, found = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int pixel = image.getColor(x,y);
            for (int color : colors) if (pixel == color) {
                left = Math.min(left,x); top = Math.min(top,y); right = Math.max(right,x); bottom = Math.max(bottom,y); found = color;
            }
        }
        return right < 0 ? null : new Box(left, top, right-left+1, bottom-top+1, found);
    }
    private void loadPack() throws Exception {
        cleanup(); reload(); sample.getOrCreateNbt().remove("TooltipStyle");
        Path example = run.getParent().resolve("examples/animated-decoration-pack"), target = run.resolve("resourcepacks/tooltip-studio-animation-smoke.zip");
        Files.createDirectories(target.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(target)); var paths = Files.walk(example)) {
            for (var p : paths.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(example.relativize(p).toString().replace('\\','/'))); Files.copy(p,zip); zip.closeEntry();
            }
        }
        reloadPacks(true).thenRun(() -> {
            var selected = TooltipStudioClient.CONFIG.select(sample);
            check(selected.inlineTextures().size() == 1 && selected.decorations().size() == 1
                            && selected.inlineTextures().get(0).equals(selected.decorations().get(0).texture()),
                    "real ZIP loads inline and independent animations sharing one uploaded atlas");
            try { check(Arrays.equals(originalConfig, Files.readAllBytes(config)), "animation pack does not rewrite local config"); }
            catch (Exception e) { throw new IllegalStateException(e); }
            phase = 3; frames = 0; minecraft.setScreen(this);
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
        phase = 4; System.err.println("ANIMATION SMOKE FAILED"); error.printStackTrace();
        try { cleanup(); } catch (Exception e) { e.printStackTrace(); }
        reloadPacks(false).whenComplete((ignored,e) -> minecraft.scheduleStop());
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); System.out.println("SMOKE PASS: " + message); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
