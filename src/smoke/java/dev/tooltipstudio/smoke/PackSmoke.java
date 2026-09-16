package dev.tooltipstudio.smoke;

import com.google.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Enables a real ZIP resource pack, exercises F3+T's reload path, then removes it before other smoke tests. */
final class PackSmoke extends Screen {
    private static final String BASE = "file/tooltip-studio-pack-smoke.zip";
    private static final String HIGH = "file/tooltip-studio-pack-override-14";
    private final MinecraftClient minecraft;
    private final Runnable after;
    private final List<String> originalPacks;
    private final Path config;
    private final byte[] originalConfig;
    private final String fallbackStyle, fallbackTexture;
    private final Path highStyle;
    private final String validHighStyle;
    private final AtomicBoolean saved = new AtomicBoolean();
    private final ItemStack[] samples = {new ItemStack(Items.STICK), forest(), new ItemStack(Items.DIAMOND)};
    private SmokeClient.SlotHarness slots;
    private int frames;
    private boolean finishing;

    private PackSmoke(MinecraftClient client, Runnable after) throws Exception {
        super(Text.literal("Tooltip Studio resource pack smoke"));
        minecraft = client;
        this.after = after;
        originalPacks = List.copyOf(client.getResourcePackManager().getEnabledNames());
        Path run = client.runDirectory.toPath().toAbsolutePath().normalize();
        config = run.resolve("config/tooltipstudio/config.json");
        originalConfig = Files.readAllBytes(config);
        fallbackStyle = "default";
        fallbackTexture = selected(samples[2]).texture();
        Path example = run.getParent().resolve("examples/resource-pack");
        Path packFolder = run.resolve("resourcepacks");
        Files.createDirectories(packFolder);
        try (var zip = new ZipOutputStream(Files.newOutputStream(packFolder.resolve("tooltip-studio-pack-smoke.zip")));
             var paths = Files.walk(example)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(example.relativize(path).toString().replace('\\', '/')));
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
        Path high = packFolder.resolve("tooltip-studio-pack-override-14");
        highStyle = high.resolve("assets/tooltipstudio/styles/monumenta/forest.json");
        Files.createDirectories(highStyle.getParent());
        Files.writeString(high.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":22,\"description\":\"Override smoke test\"}}");
        var style = JsonParser.parseString(Files.readString(example.resolve("assets/tooltipstudio/styles/monumenta/forest.json"))).getAsJsonObject();
        style.addProperty("texture", "tooltipstudio:textures/styles/default.png");
        style.addProperty("minWidth", 180);
        validHighStyle = style.toString();
        Files.writeString(highStyle, validHighStyle);
        style.addProperty("minWidth", 140);
        Path fallbackOverride = high.resolve("assets/tooltipstudio/styles/" + fallbackStyle + ".json");
        Files.createDirectories(fallbackOverride.getParent());
        Files.writeString(fallbackOverride, style.toString());
        Path rules = high.resolve("assets/tooltipstudio/rules/example.json");
        Files.createDirectories(rules.getParent());
        Files.writeString(rules, "{\"schemaVersion\":1,\"rules\":[]}");
        title(samples[0], "资源包：普通木棍", "items -> monumenta/forest");
        title(samples[1], "分类样式：森林物品", "style = monumenta/forest");
        title(samples[2], "默认样式：普通钻石", "No pack rule -> " + fallbackStyle);
    }

    static void start(MinecraftClient client, Runnable after) {
        try { new PackSmoke(client, after).run(); }
        catch (Exception failure) { failure.printStackTrace(); client.scheduleStop(); throw new IllegalStateException("PACK SMOKE FAILED", failure); }
    }

    private void run() {
        reload(BASE).thenRun(() -> {
            verifyBase();
            testLocalRules();
        }).thenCompose(ignored -> reload(BASE, HIGH)).thenRun(() -> {
            check(selected(samples[0]).minWidth() == 140, "resource-pack style overrides same-name local sample JSON");
            check(selected(forest()).minWidth() == 140, "higher pack empty rule file replaces lower file");
            var explicit = new ItemStack(Items.DIAMOND);
            explicit.getOrCreateNbt().putString("TooltipStyle", "monumenta/forest");
            check(selected(explicit).minWidth() == 180, "higher resource pack wins for the same style JSON");
            explicit.getOrCreateNbt().putString("TooltipStyle", "default");
            check(selected(explicit).minWidth() == 140, "nested pack override leaves base style independent");
            try { Files.writeString(highStyle, "{broken-json"); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }).thenCompose(ignored -> reload(BASE, HIGH)).thenRun(() -> {
            check(TooltipStudioClient.CONFIG.lastError() != null && TooltipStudioClient.CONFIG.lastError().contains("styles/monumenta/forest.json"),
                    "invalid pack JSON reports its resource path after full resource reload");
            check(TooltipStudioClient.CONFIG.count() == 2 && selected(samples[0]).minWidth() == 140,
                    "failed pack reload retains previous working styles and rules");
            try {
                JsonObject missing = JsonParser.parseString(validHighStyle).getAsJsonObject();
                missing.addProperty("texture", "tooltipstudio:textures/styles/does_not_exist.png");
                Files.writeString(highStyle, missing.toString());
                check(!TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()) && selected(samples[0]).minWidth() == 140,
                        "missing pack texture also retains the previous snapshot");
                Files.writeString(highStyle, validHighStyle);
            } catch (Exception e) { throw new IllegalStateException(e); }
        }).thenCompose(ignored -> reload(BASE)).thenRun(() -> {
            verifyBase();
            check(TooltipStudioClient.CONFIG.lastError() == null, "valid pack reload recovers after errors");
            minecraft.setScreen(this);
        }).exceptionally(failure -> { fail(failure); return null; });
    }

    private CompletableFuture<Void> reload(String... extra) {
        var manager = minecraft.getResourcePackManager();
        manager.scanPacks();
        var names = new ArrayList<>(originalPacks);
        names.addAll(List.of(extra));
        for (String name : extra) check(manager.getProfile(name) != null, "discovered resource pack " + name);
        manager.setEnabledProfiles(names);
        return minecraft.reloadResources();
    }

    private void verifyBase() {
        check(TooltipStudioClient.CONFIG.count() == 2 && TooltipStudioClient.CONFIG.styleNames().contains("monumenta/forest"),
                "ZIP resource pack adds one categorized style using the default atlas");
        check(selected(samples[0]).offsetY() == -12 && selected(samples[1]).offsetY() == -12,
                "resource pack supplies item ID and Monumenta NBT matching rules");
        check(selected(samples[2]).texture().equals(fallbackTexture) && selected(samples[2]).offsetY() == 0, "unmatched items retain existing default style");
        check(selected(samples[1]).offsetX() == 0 && selected(samples[1]).offsetY() == -12,
                "resource-pack style supplies optional whole-tooltip offsets");
    }

    private void testLocalRules() {
        try {
            var json = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
            json.getAsJsonArray("rules").add(JsonParser.parseString("""
                    {"style":"default","priority":190,"items":["minecraft:stick"]}
                    """));
            Files.writeString(config, json.toString());
            check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "local and resource-pack rules can reload together");
            check(selected(samples[0]).offsetY() == 0, "local rule wins a priority tie against pack rule");
            var explicit = new ItemStack(Items.STICK);
            explicit.getOrCreateNbt().putString("TooltipStyle", "monumenta/forest");
            check(selected(explicit).offsetY() == -12, "TooltipStyle can select a resource-pack style path");
        } catch (Exception e) { throw new IllegalStateException(e); }
        finally {
            try { Files.write(config, originalConfig); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }
        check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "original local rules restored");
    }

    private static dev.tooltipstudio.config.Style selected(ItemStack stack) { return TooltipStudioClient.CONFIG.select(stack).style(); }
    private static ItemStack forest() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.getOrCreateSubNbt("Monumenta").putString("Location", "forest");
        return stack;
    }
    private static void title(ItemStack stack, String name, String lore) {
        stack.setCustomName(Text.literal(name).styled(s -> s.withItalic(false)));
        NbtList list = new NbtList();
        list.add(NbtString.of(Text.Serialization.toJsonString(Text.literal(lore).formatted(Formatting.GRAY).styled(s -> s.withItalic(false)))));
        stack.getOrCreateSubNbt("display").put("Lore", list);
    }
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        System.out.println("SMOKE PASS: " + message);
    }
    @Override protected void init() {
        slots = new SmokeClient.SlotHarness();
        slots.init(minecraft, width, height);
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xff17202e);
        context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.6 / 分类路径实测", width / 2, 16, 0xffffff);
        context.drawCenteredTextWithShadow(textRenderer, "样式 JSON + PNG + 匹配规则全部来自已启用的 ZIP 资源包", width / 2, 35, 0xa8c5dd);
        for (int i = 0; i < samples.length; i++) {
            int y = 95 + i * 108;
            context.drawItem(samples[i], 60, y + 14);
            slots.show(context, samples[i], 92, y + 24);
        }
        context.draw();
        if (frames++ == 30) ScreenshotRecorder.saveScreenshot(minecraft.runDirectory, "tooltip-studio-resource-pack.png",
                minecraft.getFramebuffer(), message -> saved.set(true));
        if (saved.get() && !finishing) {
            finishing = true;
            reload().thenRun(() -> {
                check(TooltipStudioClient.CONFIG.count() == 1 && !TooltipStudioClient.CONFIG.styleNames().contains("monumenta/forest"),
                        "disabling resource packs removes their styles and rules");
                check(selected(samples[0]).texture().equals(fallbackTexture), "disabling pack restores original tooltip selection");
                try { check(Arrays.equals(originalConfig, Files.readAllBytes(config)), "pack loading leaves local config file unchanged"); }
                catch (Exception e) { throw new IllegalStateException(e); }
                after.run();
            }).exceptionally(failure -> { fail(failure); return null; });
        }
        if (frames > 1200 && !finishing) throw new IllegalStateException("Pack screenshot timed out");
    }
    private void fail(Throwable failure) {
        System.err.println("PACK SMOKE FAILED");
        failure.printStackTrace();
        try { Files.write(config, originalConfig); }
        catch (Exception e) { e.printStackTrace(); }
        reload().whenComplete((ignored, cleanupFailure) -> minecraft.scheduleStop());
    }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
