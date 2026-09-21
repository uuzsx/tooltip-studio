package dev.tooltipstudio.smoke;

import com.google.gson.JsonParser;
import dev.tooltipstudio.TooltipStudioClient;
import dev.tooltipstudio.render.TooltipRenderScope;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public final class SmokeClient implements ClientModInitializer {
    private boolean started;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!started && client.currentScreen instanceof TitleScreen && client.getOverlay() == null) {
                started = true;
                try {
                    if (Boolean.getBoolean("tooltipstudio.poster")) {
                        client.options.getGuiScale().setValue(2);
                        client.onResolutionChanged();
                        BaseStyleSmoke.start(client, client::scheduleStop);
                        return;
                    }
                    check(TooltipStudioClient.CONFIG.count() == 1, "only the default base style is bundled");
                    var sword = new ItemStack(Items.NETHERITE_SWORD);
                    check(TooltipStudioClient.CONFIG.select(sword).style().texture().contains("default"), "ordinary item uses the only bundled style");
                    sword.getOrCreateNbt().putString("TooltipStyle", "default");
                    check(TooltipStudioClient.CONFIG.select(sword).style().texture().contains("default"), "NBT override");
                    testReload(client);
                    testNestedStyles(client);
                    client.options.getGuiScale().setValue(2);
                    client.onResolutionChanged();
                    client.reloadResources().thenRun(() -> {
                        check(TooltipStudioClient.CONFIG.count() == 1, "resource reload retained all styles");
                        BaseStyleSmoke.start(client, () -> PackSmoke.start(client,
                                () -> DecorationSmoke.start(client, () -> AdvancedDecorationSmoke.start(client,
                                        () -> SegmentedTextSmoke.start(client, () -> AnimatedDecorationSmoke.start(client,
                                                () -> OffsetSmoke.start(client, () -> NbtSmoke.start(client, new Gallery()))))))));
                    }).exceptionally(failure -> {
                        failure.printStackTrace();
                        client.scheduleStop();
                        return null;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    client.scheduleStop();
                    throw new IllegalStateException("SMOKE FAILED", e);
                }
            }
        });
    }

    private static void check(boolean passed, String name) {
        if (!passed) throw new AssertionError(name);
        System.out.println("SMOKE PASS: " + name);
    }

    static com.google.gson.JsonObject baseDefinition() {
        try (var reader = new java.io.InputStreamReader(SmokeClient.class.getResourceAsStream(
                "/assets/tooltipstudio/defaults/styles/default.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private void testReload(MinecraftClient client) throws Exception {
        Path config = client.runDirectory.toPath().resolve("config/tooltipstudio");
        byte[] good = Files.readAllBytes(config.resolve("config.json"));
        try {
            Files.writeString(config.resolve("config.json"), "{broken-json", StandardCharsets.UTF_8);
            check(!TooltipStudioClient.CONFIG.reload(client.getResourceManager()), "bad JSON rejected");
            check(TooltipStudioClient.CONFIG.count() == 1, "last working configuration retained");
        } finally { Files.write(config.resolve("config.json"), good); }
        check(TooltipStudioClient.CONFIG.reload(client.getResourceManager()), "reload recovered");
        try (var input = getClass().getResourceAsStream("/assets/tooltipstudio/textures/styles/default.png")) {
            Files.copy(input, config.resolve("textures/custom.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Path custom = config.resolve("styles/custom.json");
        var json = baseDefinition();
        json.addProperty("texture", "local:custom.png");
        Files.writeString(custom, json.toString());
        try {
            check(TooltipStudioClient.CONFIG.reload(client.getResourceManager()) && TooltipStudioClient.CONFIG.count() == 2,
                    "custom style and local PNG loaded");
            var item = new ItemStack(Items.DIAMOND);
            item.getOrCreateNbt().putString("TooltipStyle", "custom");
            check(TooltipStudioClient.CONFIG.select(item).style().texture().equals("local:custom.png"), "custom style selected");
        } finally { Files.deleteIfExists(custom); }
        check(TooltipStudioClient.CONFIG.reload(client.getResourceManager()), "sample configuration restored");
    }

    private void testNestedStyles(MinecraftClient client) throws Exception {
        Path config = client.runDirectory.toPath().resolve("config/tooltipstudio");
        Path settings = config.resolve("config.json");
        byte[] original = Files.readAllBytes(settings);
        Path first = config.resolve("styles/smoke_groups/a/forest.json");
        Path second = config.resolve("styles/smoke_groups/b/deep/forest.json");
        try {
            Files.createDirectories(first.getParent());
            Files.createDirectories(second.getParent());
            var style = baseDefinition();
            style.addProperty("texture", "local:custom.png");
            style.addProperty("minWidth", 131);
            Files.writeString(first, style.toString());
            style.addProperty("minWidth", 141);
            Files.writeString(second, style.toString());
            var json = JsonParser.parseString(Files.readString(settings)).getAsJsonObject();
            json.addProperty("defaultStyle", "smoke_groups/b/deep/forest");
            json.getAsJsonArray("rules").add(JsonParser.parseString("""
                    {"style":"smoke_groups/a/forest","priority":500,"items":["minecraft:stick"]}
                    """));
            Files.writeString(settings, json.toString());
            check(TooltipStudioClient.CONFIG.reload(client.getResourceManager()), "nested local style reload");
            check(TooltipStudioClient.CONFIG.count() == 3, "same basename in different local folders is distinct");
            check(TooltipStudioClient.CONFIG.select(new ItemStack(Items.STICK)).style().minWidth() == 131,
                    "local rule selects full style path");
            check(TooltipStudioClient.CONFIG.select(new ItemStack(Items.DIAMOND)).style().minWidth() == 141,
                    "defaultStyle selects a multi-level local path");
            var item = new ItemStack(Items.DIAMOND);
            item.getOrCreateNbt().putString("TooltipStyle", "smoke_groups/a/forest");
            check(TooltipStudioClient.CONFIG.select(item).style().minWidth() == 131, "NBT override selects local path");
            Files.delete(first);
            check(!TooltipStudioClient.CONFIG.reload(client.getResourceManager())
                    && TooltipStudioClient.CONFIG.select(item).style().minWidth() == 131,
                    "missing nested style reference retains previous snapshot");
        } finally {
            Files.write(settings, original);
            Files.deleteIfExists(first);
            Files.deleteIfExists(second);
        }
        check(TooltipStudioClient.CONFIG.reload(client.getResourceManager()) && TooltipStudioClient.CONFIG.count() == 1,
                "nested local styles are removed after restoring files and references");
    }

    private static ItemStack sample(String style, String title, String... lore) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.setCustomName(Text.literal(title));
        stack.getOrCreateNbt().putString("TooltipStyle", style);
        stack.getOrCreateNbt().putInt("HideFlags", 127);
        NbtList list = new NbtList();
        for (String line : lore) list.add(NbtString.of(Text.Serialization.toJsonString(Text.literal(line))));
        stack.getOrCreateSubNbt("display").put("Lore", list);
        return stack;
    }

    private static final class Gallery extends Screen {
        private final String[] styles = {"default"};
        private final ItemStack[] samples = new ItemStack[styles.length];
        private int frames;
        private final AtomicInteger saved = new AtomicInteger();
        private SlotHarness slots;
        private ShulkerSmoke shulker;
        private Gallery() {
            super(Text.literal("Tooltip Studio smoke test"));
            for (int i = 0; i < styles.length; i++) samples[i] = sample(styles[i], "Centered item name", "A configurable lore line.", "One atlas per style.");
        }
        @Override protected void init() {
            slots = new SlotHarness();
            slots.init(client, width, height);
            if (TooltipRenderScope.SHULKER_LOADED) shulker = new ShulkerSmoke(client, width, height);
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(0, 0, width, height, 0xff17202e);
            context.drawCenteredTextWithShadow(textRenderer, "TOOLTIP STUDIO / Fabric 1.20.4", width / 2, 8, 0xffffff);
            if (frames < 45) {
                for (int i = 0; i < styles.length; i++) {
                    int x = 14 + (i % 3) * (width / 3);
                    int y = 46 + (i / 3) * 127;
                    context.drawTextWithShadow(textRenderer, styles[i], x, y - 15, 0xa8b8d0);
                    if (i < 6) slots.show(context, samples[i], x - 12, y + 12);
                    else context.drawItemTooltip(textRenderer, samples[i], x - 12, y + 12);
                }
            } else if (frames < 90) {
                context.drawTextWithShadow(textRenderer, "Long title, wrapping, fixed caps, title-only and screen edges", 10, 30, 0xa8b8d0);
                context.drawItemTooltip(textRenderer, sample("default", "Name"), 4, 65);
                slots.show(context, sample("default", "A very long item name that wraps across several visible lines and stays centered",
                        "This is a long lore line. It wraps within the configured maximum width while preserving the item tooltip layout.",
                        "Second lore line."), width - 4, 130);
                context.drawItemTooltip(textRenderer, sample("default", "屏幕边缘测试", "物品名称居中", "分割线仅拉伸中间部分"), width - 4, height - 4);
                ItemStack bundle = new ItemStack(Items.BUNDLE);
                NbtList items = new NbtList();
                items.add(new ItemStack(Items.DIAMOND, 8).writeNbt(new net.minecraft.nbt.NbtCompound()));
                bundle.getOrCreateNbt().put("Items", items);
                context.drawItemTooltip(textRenderer, bundle, 20, 220);
            } else if (frames < 125 || shulker == null) {
                String[] lore = new String[60];
                for (int i = 0; i < lore.length; i++) lore[i] = "Lore line " + (i + 1);
                slots.show(context, sample("default", "Tall tooltip", lore), 15, height - 10);
                slots.show(context, sample("default", "Centered wrapped title: a long name that spans multiple lines",
                        "All 60 lines fit on screen.", "The entire tall tooltip scales together."), width - 5, 180);
            } else {
                shulker.render(context, textRenderer);
            }
            context.draw();
            if (frames == 10) {
                try {
                    context.drawItemTooltip(null, samples[0], 10, 10);
                    throw new AssertionError("Expected invalid renderer to fail");
                } catch (NullPointerException expected) {
                    check(TooltipRenderScope.item() == null, "item scope restored after render exception");
                }
            }
            if (frames == 30 || frames == 75 || frames == 105) {
                String file = frames == 30 ? "tooltip-studio-styles.png" : frames == 75 ? "tooltip-studio-layout.png" : "tooltip-studio-tall.png";
                ScreenshotRecorder.saveScreenshot(client.runDirectory, file, client.getFramebuffer(), message -> saved.incrementAndGet());
                System.out.println("SMOKE CAPTURE: " + file);
            }
            if (frames == 160 && shulker != null) {
                ScreenshotRecorder.saveScreenshot(client.runDirectory, "tooltip-studio-shulker-compat.png",
                        client.getFramebuffer(), message -> saved.incrementAndGet());
                System.out.println("SMOKE PASS: Shulker previews inside/outside, lock selection, nested scope and release");
            }
            frames++;
            if (frames > (shulker == null ? 120 : 180) && saved.get() == (shulker == null ? 3 : 4)) {
                if (shulker != null) shulker.close(client);
                System.out.println("SMOKE COMPLETE");
                client.scheduleStop();
            }
            if (frames > 600) throw new IllegalStateException("Screenshot capture timed out");
        }
        @Override public boolean shouldPause() { return false; }
    }

    /** Exercise the real inventory tooltip entry point without creating a world/save. */
    static final class SlotHarness extends HandledScreen<EmptyHandler> {
        private final SimpleInventory inventory = new SimpleInventory(2);
        SlotHarness() { super(new EmptyHandler(), new PlayerInventory(null), Text.literal("Test inventory")); }
        void show(DrawContext context, ItemStack stack, int x, int y) {
            showSlot(context, 0, stack, x, y);
        }
        void showSlot(DrawContext context, int index, ItemStack stack, int x, int y) {
            inventory.setStack(index, stack);
            focusedSlot = new Slot(inventory, index, 0, 0);
            drawMouseoverTooltip(context, x, y);
            if (TooltipRenderScope.item() != null) throw new AssertionError("Inventory tooltip scope leaked");
        }
        @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {}
    }
    private static final class EmptyHandler extends ScreenHandler {
        EmptyHandler() { super(null, 0); }
        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return true; }
    }
}
