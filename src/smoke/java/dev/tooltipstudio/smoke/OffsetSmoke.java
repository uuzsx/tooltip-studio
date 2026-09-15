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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/** Compares real framebuffer pixels to verify that every part moves by the same screen offset. */
final class OffsetSmoke extends Screen {
    private final MinecraftClient minecraft;
    private final Runnable after;
    private final Path baseFile, shiftedFile;
    private final JsonObject definition;
    private final ItemStack base = item("smoke_offsets/base", false);
    private final ItemStack shifted = item("smoke_offsets/shifted", false);
    private final ItemStack tall = item("smoke_offsets/shifted", true);
    private final AtomicBoolean saved = new AtomicBoolean();
    private SmokeClient.SlotHarness slots;
    private NativeImage baseline, tallBaseline;
    private int frames;

    private OffsetSmoke(MinecraftClient client, Runnable after) throws Exception {
        super(Text.literal("Tooltip offset verification"));
        minecraft = client;
        this.after = after;
        Path config = client.runDirectory.toPath().resolve("config/tooltipstudio/styles");
        baseFile = config.resolve("smoke_offsets/base.json");
        shiftedFile = config.resolve("smoke_offsets/shifted.json");
        Files.createDirectories(baseFile.getParent());
        definition = SmokeClient.baseDefinition();
        definition.addProperty("offsetX", 0);
        definition.addProperty("offsetY", 0);
        Files.writeString(baseFile, definition.toString());
        setOffset(0, 0);
    }

    static void start(MinecraftClient client, Runnable after) {
        try { client.setScreen(new OffsetSmoke(client, after)); }
        catch (Exception e) { e.printStackTrace(); client.scheduleStop(); throw new IllegalStateException("OFFSET SMOKE FAILED", e); }
    }

    private static ItemStack item(String style, boolean tall) {
        var stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.setCustomName(Text.literal("整体偏移示例"));
        stack.getOrCreateNbt().putString("TooltipStyle", style);
        stack.getOrCreateNbt().putInt("HideFlags", 127);
        NbtList lore = new NbtList();
        for (int i = 0; i < (tall ? 60 : 2); i++) {
            String line = tall ? "Tall tooltip line " + (i + 1) : i == 0 ? "Frame / title / divider / lore" : "边框、文字和装饰一起移动";
            lore.add(NbtString.of(Text.Serialization.toJsonString(Text.literal(line).styled(s -> s.withItalic(false)))));
        }
        stack.getOrCreateSubNbt("display").put("Lore", lore);
        return stack;
    }

    private void setOffset(int x, int y) throws Exception {
        definition.addProperty("offsetX", x);
        definition.addProperty("offsetY", y);
        Files.writeString(shiftedFile, definition.toString());
        check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "offset JSON hot reload");
        var selected = TooltipStudioClient.CONFIG.select(shifted).style();
        check(selected.offsetX() == x && selected.offsetY() == y, "selected style retains both offset parameters");
    }

    @Override protected void init() {
        slots = new SmokeClient.SlotHarness();
        slots.init(minecraft, width, height);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try {
            context.fill(0, 0, width, height, 0xff17202e);
            if (frames <= 105) {
                slots.show(context, frames > 75 ? tall : shifted, 70, 180);
            } else {
                context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.4 / 整体 XY 偏移", width / 2, 20, 0xffffff);
                context.drawCenteredTextWithShadow(textRenderer, "相同悬停高度；右侧向右 24、向上 20 个界面像素", width / 2, 42, 0xa8c5dd);
                context.drawTextWithShadow(textRenderer, "offsetX: 0   offsetY: 0", 28, 100, 0xffffff);
                context.drawTextWithShadow(textRenderer, "offsetX: 24   offsetY: -20", 332, 100, 0xffffff);
                context.fill(20, 168, width - 20, 169, 0xff506078);
                slots.show(context, base, 24, 180);
                slots.show(context, shifted, 328, 180);
            }
            context.draw();
            if (frames == 15) {
                baseline = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer());
                setOffset(24, -20);
            } else if (frames == 30) {
                verifyTranslation(baseline, 24, -20);
                setOffset(-16, 18);
            } else if (frames == 45) {
                verifyTranslation(baseline, -16, 18);
                setOffset(4096, 4096);
            } else if (frames == 60) {
                verifyBounds();
                setOffset(-4096, -4096);
            } else if (frames == 75) {
                verifyBounds();
                definition.addProperty("offsetY", 4097);
                Files.writeString(shiftedFile, definition.toString());
                check(!TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                                && TooltipStudioClient.CONFIG.select(shifted).style().offsetY() == -4096,
                        "invalid offset rejects reload and preserves previous style");
                setOffset(0, 0);
            } else if (frames == 90) {
                tallBaseline = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer());
                setOffset(24, 0);
            } else if (frames == 105) {
                verifyTranslation(tallBaseline, 24, 0);
                setOffset(24, -20);
            } else if (frames == 120) {
                ScreenshotRecorder.saveScreenshot(minecraft.runDirectory, "tooltip-studio-offset.png",
                        minecraft.getFramebuffer(), message -> saved.set(true));
            }
            if (saved.get()) {
                cleanup();
                check(TooltipStudioClient.CONFIG.count() == 1, "offset smoke restores original style files");
                after.run();
            }
            if (++frames > 1200) throw new IllegalStateException("Offset screenshot timed out");
        } catch (Exception | AssertionError e) {
            System.err.println("OFFSET SMOKE FAILED");
            e.printStackTrace();
            try { cleanup(); } catch (Exception cleanupFailure) { cleanupFailure.printStackTrace(); }
            minecraft.scheduleStop();
        }
    }

    private void verifyTranslation(NativeImage before, int offsetX, int offsetY) {
        try (var now = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            int dx = Math.round(offsetX * before.getWidth() / (float) width);
            int dy = Math.round(offsetY * before.getHeight() / (float) height);
            int background = before.getColor(0, 0), changed = 0, differences = 0;
            for (int y = 0; y < now.getHeight(); y++) for (int x = 0; x < now.getWidth(); x++) {
                int sx = x - dx, sy = y - dy;
                int expected = sx >= 0 && sx < before.getWidth() && sy >= 0 && sy < before.getHeight()
                        ? before.getColor(sx, sy) : background;
                if (expected != background) changed++;
                if (now.getColor(x, y) != expected) differences++;
            }
            check(changed > 100 && differences == 0, "whole framebuffer tooltip translates exactly by " + offsetX + ", " + offsetY
                    + " GUI pixels (mismatched pixels: " + differences + ")");
        }
    }

    private void verifyBounds() {
        try (var image = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
            int background = image.getColor(0, 0), pixels = 0;
            int marginX = 4 * image.getWidth() / width, marginY = 4 * image.getHeight() / height;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                if (image.getColor(x, y) == background) continue;
                pixels++;
                checkPixel(x >= marginX && x < image.getWidth() - marginX
                        && y >= marginY && y < image.getHeight() - marginY);
            }
            check(pixels > 100, "extreme offsets keep tooltip and decorations inside all screen edges");
        }
    }

    private static void checkPixel(boolean visible) {
        if (!visible) throw new AssertionError("Tooltip pixel escaped screen margin");
    }

    private void cleanup() throws Exception {
        if (baseline != null) { baseline.close(); baseline = null; }
        if (tallBaseline != null) { tallBaseline.close(); tallBaseline = null; }
        Files.deleteIfExists(baseFile);
        Files.deleteIfExists(shiftedFile);
        check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "original configuration restored after offset smoke");
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        System.out.println("SMOKE PASS: " + message);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
