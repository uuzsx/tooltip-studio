package dev.tooltipstudio.smoke;

import com.google.gson.JsonParser;
import dev.tooltipstudio.TooltipStudioClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** Verifies the base style on new and existing configurations without replacing user settings. */
final class BaseStyleSmoke extends Screen {
    private final MinecraftClient minecraft;
    private final Runnable after;
    private final Path config, baseFile;
    private final byte[] originalConfig, originalBase;
    private final AtomicBoolean saved = new AtomicBoolean();
    private final ItemStack stick = new ItemStack(Items.STICK);
    private final ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
    private final ItemStack book = new ItemStack(Items.BOOK);
    private SmokeClient.SlotHarness slots;
    private int frames;
    private boolean finishing;

    private BaseStyleSmoke(MinecraftClient client, Runnable after) throws Exception {
        super(Text.literal("Default tooltip style"));
        minecraft = client;
        this.after = after;
        Path directory = client.runDirectory.toPath().resolve("config/tooltipstudio");
        config = directory.resolve("config.json");
        baseFile = directory.resolve("styles/default.json");
        originalConfig = Files.readAllBytes(config);
        originalBase = Files.exists(baseFile) ? Files.readAllBytes(baseFile) : null;
        name(sword, "基础样式 · 钻石剑", "名称居中，分割线两端固定", "边框、背景与分割线来自同一张贴图");
        name(book, "基础样式 · 冒险笔记", "当物品说明比较长时，会按照配置的最大宽度自动折行，继续保留名称居中与独立分割线；背景和边框会随着正文高度一起扩展，让多行文字也能完整地显示。",
                "更长的说明会自动换行，名称与正文之间保留独立分割线。");
    }

    static void start(MinecraftClient client, Runnable after) {
        BaseStyleSmoke screen = null;
        try {
            screen = new BaseStyleSmoke(client, after);
            screen.verify();
            client.setScreen(screen);
        } catch (Exception | AssertionError e) {
            if (screen != null) screen.restore();
            e.printStackTrace();
            client.scheduleStop();
            throw new IllegalStateException("BASE STYLE SMOKE FAILED", e);
        }
    }

    private void verify() throws Exception {
        String originalTexture = TooltipStudioClient.CONFIG.select(stick).style().texture();
        Files.deleteIfExists(baseFile);
        check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "base style is available without a local JSON on upgrade");
        check(TooltipStudioClient.CONFIG.select(stick).style().texture().equals(originalTexture)
                && Arrays.equals(originalConfig, Files.readAllBytes(config)) && !Files.exists(baseFile),
                "built-in fallback does not modify existing config or create local style files");
        var explicit = new ItemStack(Items.DIAMOND);
        explicit.getOrCreateNbt().putString("TooltipStyle", "default");
        check(TooltipStudioClient.CONFIG.select(explicit).style().texture().endsWith("/default.png"),
                "default style can be selected explicitly on upgrade");

        var settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
        settings.addProperty("defaultStyle", "rare");
        Files.writeString(config, settings.toString());
        check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                && TooltipStudioClient.CONFIG.select(stick).style().texture().endsWith("/default.png"),
                "a retired default name safely resolves to the base");
        settings.addProperty("defaultStyle", "default");
        Files.writeString(config, settings.toString());
        check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                && TooltipStudioClient.CONFIG.select(stick).style().texture().endsWith("/default.png"),
                "ordinary items use the new base after selecting default");

        try (var input = getClass().getResourceAsStream("/assets/tooltipstudio/defaults/styles/default.json")) {
            var custom = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            custom.addProperty("minWidth", 145);
            Files.writeString(baseFile, custom.toString());
        }
        check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                && TooltipStudioClient.CONFIG.select(stick).style().minWidth() == 145,
                "local default JSON overrides the built-in base");
        Files.delete(baseFile);
        check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                && TooltipStudioClient.CONFIG.select(stick).style().minWidth() == 100,
                "removing the local base override restores the bundled base");
    }

    private static void name(ItemStack stack, String title, String... lines) {
        stack.setCustomName(Text.literal(title).styled(s -> s.withItalic(false)));
        stack.getOrCreateNbt().putInt("HideFlags", 127);
        NbtList lore = new NbtList();
        for (String line : lines) lore.add(NbtString.of(Text.Serialization.toJsonString(
                Text.literal(line).formatted(Formatting.GRAY).styled(s -> s.withItalic(false)))));
        stack.getOrCreateSubNbt("display").put("Lore", lore);
    }

    @Override protected void init() {
        slots = new SmokeClient.SlotHarness();
        slots.init(minecraft, width, height);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xff354047);
        context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.5 / 新默认基础样式", width / 2, 22, 0xffe5dfce);
        context.drawCenteredTextWithShadow(textRenderer, "原样使用朋友提供的 PNG 与切片参数", width / 2, 44, 0xffb9c0c2);
        context.drawTextWithShadow(textRenderer, "只有名称", 30, 88, 0xffb9c0c2);
        context.drawItem(stick, 30, 128);
        slots.show(context, stick, 54, 136);
        context.drawTextWithShadow(textRenderer, "名称 + 分割线 + lore", 294, 88, 0xffb9c0c2);
        context.drawItem(sword, 294, 128);
        slots.show(context, sword, 318, 136);
        context.drawTextWithShadow(textRenderer, "长文本与自动换行", 30, 244, 0xffb9c0c2);
        context.drawItem(book, 30, 282);
        slots.show(context, book, 54, 290);
        context.draw();
        if (frames++ == 30) ScreenshotRecorder.saveScreenshot(minecraft.runDirectory, "tooltip-studio-default.png",
                minecraft.getFramebuffer(), message -> saved.set(true));
        if (saved.get() && !finishing) {
            finishing = true;
            restore();
            after.run();
        }
        if (frames > 1200 && !finishing) throw new IllegalStateException("Base style screenshot timed out");
    }

    private void restore() {
        try {
            Files.write(config, originalConfig);
            if (originalBase == null) Files.deleteIfExists(baseFile); else Files.write(baseFile, originalBase);
            check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "base-style demo restores original config and style files");
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        System.out.println("SMOKE PASS: " + message);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
