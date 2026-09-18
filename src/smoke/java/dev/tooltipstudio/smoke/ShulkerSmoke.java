package dev.tooltipstudio.smoke;

import com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltip;
import com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltipClient;
import com.misterpemodder.shulkerboxtooltip.impl.config.Configuration.PreviewPosition;
import dev.tooltipstudio.render.TooltipRenderScope;
import dev.tooltipstudio.TooltipStudioClient;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs only with -PcompatShulker; no third-party test code is packaged into the mod. */
final class ShulkerSmoke {
    private final SmokeClient.SlotHarness inside = new SmokeClient.SlotHarness();
    private final SmokeClient.SlotHarness outside = new SmokeClient.SlotHarness();
    private final SmokeClient.SlotHarness lock = new SmokeClient.SlotHarness();
    private final ItemStack box = new ItemStack(Items.PURPLE_SHULKER_BOX);
    private final ItemStack other = new ItemStack(Items.DIAMOND);
    private final Field lockKey;
    private final Path offsetStyle;
    private final Path config, decoration, decorationPng;
    private final byte[] originalConfig;

    ShulkerSmoke(MinecraftClient client, int width, int height) {
        // SBT normally registers providers on world join; this screen test creates no world.
        com.misterpemodder.shulkerboxtooltip.impl.PluginManager.loadProviders();
        for (var screen : new SmokeClient.SlotHarness[]{inside, outside, lock}) screen.init(client, width, height);
        box.setCustomName(Text.literal("Shulker preview / offset (+16, -12)"));
        box.getOrCreateNbt().putString("TooltipStyle", "smoke_shulker_offset");
        offsetStyle = client.runDirectory.toPath().resolve("config/tooltipstudio/styles/smoke_shulker_offset.json");
        config = offsetStyle.getParent().getParent().resolve("config.json");
        decoration = config.getParent().resolve("decorations/smoke_shulker/sword.json");
        decorationPng = config.getParent().resolve("textures/smoke_shulker/sword.png");
        try {
            originalConfig = Files.readAllBytes(config);
            var json = SmokeClient.baseDefinition();
            json.addProperty("offsetX", 16);
            json.addProperty("offsetY", -12);
            json.getAsJsonArray("decorations").add(JsonParser.parseString("""
                    {"type":"text","segments":[{"text":"BOX ","color":"#AAAAAA"},{"text":"PREVIEW","color":"#FFD866"}],"shadow":true,
                     "anchor":"SEPARATOR_CENTER","foreground":true,"x_scale":0.75,"y_scale":0.75}
                    """));
            Files.writeString(offsetStyle, json.toString());
            Path example = client.runDirectory.toPath().toAbsolutePath().getParent().resolve("examples/decoration-pack/assets/tooltipstudio");
            var overlay = JsonParser.parseString(Files.readString(example.resolve("decorations/sword/top_left.json"))).getAsJsonObject();
            overlay.addProperty("texture", "local:smoke_shulker/sword.png");
            overlay.addProperty("x_scale", 1.5);
            overlay.addProperty("y_scale", 0.75);
            Files.createDirectories(decoration.getParent()); Files.createDirectories(decorationPng.getParent());
            Files.writeString(decoration, overlay.toString());
            Files.copy(example.resolve("textures/decorations/sword.png"), decorationPng, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            var settings = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
            if (!settings.has("decorationRules")) settings.add("decorationRules", new com.google.gson.JsonArray());
            settings.getAsJsonArray("decorationRules").add(JsonParser.parseString("""
                    {"decorations":["smoke_shulker/sword"],"items":["minecraft:purple_shulker_box"]}
                    """));
            Files.writeString(config, settings.toString());
            if (!TooltipStudioClient.CONFIG.reload(client.getResourceManager())) throw new IllegalStateException("Shulker offset style did not load");
            if (TooltipStudioClient.CONFIG.select(box).decorations().size() != 1) throw new AssertionError("Shulker independent overlay missing");
        } catch (Exception e) { throw new IllegalStateException(e); }
        NbtList contents = new NbtList();
        NbtCompound diamond = new ItemStack(Items.DIAMOND, 32).writeNbt(new NbtCompound());
        diamond.putByte("Slot", (byte) 0);
        NbtCompound gold = new ItemStack(Items.GOLD_INGOT, 16).writeNbt(new NbtCompound());
        gold.putByte("Slot", (byte) 1);
        contents.add(diamond); contents.add(gold);
        box.getOrCreateSubNbt("BlockEntityTag").put("Items", contents);
        ShulkerBoxTooltip.config.preview.alwaysOn = true;
        ShulkerBoxTooltip.config.tooltip.showKeyHints = false;
        try {
            lockKey = ShulkerBoxTooltipClient.class.getDeclaredField("lockPreviewKeyPressed");
            lockKey.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        if (box.getTooltipData().isEmpty()) throw new AssertionError("Shulker preview data missing");
    }

    void close(MinecraftClient client) {
        try {
            Files.deleteIfExists(offsetStyle);
            Files.deleteIfExists(decoration); Files.deleteIfExists(decorationPng); Files.write(config, originalConfig);
            if (!TooltipStudioClient.CONFIG.reload(client.getResourceManager())) throw new IllegalStateException("Shulker styles did not restore");
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    void render(DrawContext context, TextRenderer font) {
        try {
            lockKey.setBoolean(null, false);
            ShulkerBoxTooltip.config.preview.position = PreviewPosition.INSIDE;
            context.drawTextWithShadow(font, "Shulker Box Tooltip 4.1.0 / INSIDE", 15, 40, 0xffffff);
            inside.show(context, box, 10, 85);
            ShulkerBoxTooltip.config.preview.position = PreviewPosition.OUTSIDE;
            context.drawTextWithShadow(font, "OUTSIDE preview below styled tooltip", 325, 40, 0xffffff);
            outside.show(context, box, 320, 85);

            ShulkerBoxTooltip.config.preview.position = PreviewPosition.INSIDE;
            lockKey.setBoolean(null, true);
            context.drawTextWithShadow(font, "LOCK: hover changes, box keeps its offset", 15, 220, 0xffffff);
            lock.showSlot(context, 0, box, 10, 265);
            lock.showSlot(context, 1, other, 200, 280);
            var parent = TooltipRenderScope.enter(other, lock);
            try {
                if (TooltipRenderScope.item() != box) throw new AssertionError("Locked stack lost");
                if (TooltipStudioClient.CONFIG.select(TooltipRenderScope.item()).decorations().size() != 1)
                    throw new AssertionError("Locked shulker decoration lost");
                var nested = TooltipRenderScope.enter(other, null);
                try {
                    if (TooltipRenderScope.item() != other) throw new AssertionError("Nested stack lost");
                } finally { TooltipRenderScope.restore(nested); }
                if (TooltipRenderScope.item() != box) throw new AssertionError("Parent scope not restored");
            } finally { TooltipRenderScope.restore(parent); }

            lockKey.setBoolean(null, false);
            context.drawTextWithShadow(font, "RELEASE: diamond uses its default base style", 325, 220, 0xffffff);
            lock.showSlot(context, 1, other, 320, 265);
            var released = TooltipRenderScope.enter(other, lock);
            try {
                if (TooltipRenderScope.item() != other) throw new AssertionError("Lock did not release");
                if (!TooltipStudioClient.CONFIG.select(TooltipRenderScope.item()).decorations().isEmpty())
                    throw new AssertionError("Shulker decoration leaked to another item after release");
            } finally { TooltipRenderScope.restore(released); }
            context.drawTooltip(font, java.util.List.of(Text.literal("Generic tooltip stays vanilla")),
                    java.util.Optional.empty(), 330, 380);
        } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
}
