package dev.tooltipstudio.smoke;

import com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltip;
import com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltipClient;
import com.misterpemodder.shulkerboxtooltip.impl.config.Configuration.PreviewPosition;
import dev.tooltipstudio.render.TooltipRenderScope;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

/** Runs only with -PcompatShulker; no third-party test code is packaged into the mod. */
final class ShulkerSmoke {
    private final SmokeClient.SlotHarness inside = new SmokeClient.SlotHarness();
    private final SmokeClient.SlotHarness outside = new SmokeClient.SlotHarness();
    private final SmokeClient.SlotHarness lock = new SmokeClient.SlotHarness();
    private final ItemStack box = new ItemStack(Items.PURPLE_SHULKER_BOX);
    private final ItemStack other = new ItemStack(Items.DIAMOND);
    private final Field lockKey;

    ShulkerSmoke(MinecraftClient client, int width, int height) {
        // SBT normally registers providers on world join; this screen test creates no world.
        com.misterpemodder.shulkerboxtooltip.impl.PluginManager.loadProviders();
        for (var screen : new SmokeClient.SlotHarness[]{inside, outside, lock}) screen.init(client, width, height);
        box.setCustomName(Text.literal("Shulker preview / Legendary"));
        box.getOrCreateNbt().putString("TooltipStyle", "legendary");
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
            context.drawTextWithShadow(font, "LOCK: hover changes, legendary box stays", 15, 220, 0xffffff);
            lock.showSlot(context, 0, box, 10, 265);
            lock.showSlot(context, 1, other, 200, 280);
            var parent = TooltipRenderScope.enter(other, lock);
            try {
                if (TooltipRenderScope.item() != box) throw new AssertionError("Locked stack lost");
                var nested = TooltipRenderScope.enter(other, null);
                try {
                    if (TooltipRenderScope.item() != other) throw new AssertionError("Nested stack lost");
                } finally { TooltipRenderScope.restore(nested); }
                if (TooltipRenderScope.item() != box) throw new AssertionError("Parent scope not restored");
            } finally { TooltipRenderScope.restore(parent); }

            lockKey.setBoolean(null, false);
            context.drawTextWithShadow(font, "RELEASE: diamond uses its own rare style", 325, 220, 0xffffff);
            lock.showSlot(context, 1, other, 320, 265);
            var released = TooltipRenderScope.enter(other, lock);
            try {
                if (TooltipRenderScope.item() != other) throw new AssertionError("Lock did not release");
            } finally { TooltipRenderScope.restore(released); }
            context.drawTooltip(font, java.util.List.of(Text.literal("Generic tooltip stays vanilla")),
                    java.util.Optional.empty(), 330, 380);
        } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
}
