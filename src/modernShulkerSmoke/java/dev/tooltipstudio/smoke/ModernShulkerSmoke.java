package dev.tooltipstudio.smoke;

import com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltip;
import com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltipClient;
import com.misterpemodder.shulkerboxtooltip.impl.config.Configuration.PreviewPosition;
import dev.tooltipstudio.render.TooltipRenderScope;
import dev.tooltipstudio.TooltipStudioClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

/** Runs only with -PcompatShulker; no third-party test code is packaged into the mod. */
final class ModernShulkerSmoke {
    private final ModernSmokeClient.SlotHarness inside = new ModernSmokeClient.SlotHarness();
    private final ModernSmokeClient.SlotHarness outside = new ModernSmokeClient.SlotHarness();
    private final ModernSmokeClient.SlotHarness lock = new ModernSmokeClient.SlotHarness();
    private final ItemStack box = new ItemStack(Items.PURPLE_SHULKER_BOX);
    private final ItemStack other = new ItemStack(Items.DIAMOND);
    private final Field lockKey;
    private static boolean providerRegistered;
    ModernShulkerSmoke(MinecraftClient client, int width, int height) {
        com.misterpemodder.shulkerboxtooltip.impl.PluginManager.loadProviders();
        // This screen harness deliberately has no world. Supply a real registry lookup to the
        // ordinary SBT container provider; retain its normal component decoding and renderer.
        if (!providerRegistered) {
            var lookup = net.minecraft.registry.BuiltinRegistries.createWrapperLookup();
            var registry = com.misterpemodder.shulkerboxtooltip.impl.provider.PreviewProviderRegistryImpl.INSTANCE;
            registry.setLocked(false);
            try {
                registry.register(net.minecraft.util.Identifier.of("tooltipstudio_smoke", "container"),
                        new com.misterpemodder.shulkerboxtooltip.api.provider.BlockEntityPreviewProvider(27, false) {
                            @Override public int getPriority() { return Integer.MAX_VALUE; }
                            @Override public java.util.List<ItemStack> getInventory(com.misterpemodder.shulkerboxtooltip.api.PreviewContext context) {
                                return super.getInventory(com.misterpemodder.shulkerboxtooltip.api.PreviewContext.builder(context.stack())
                                        .withRegistryLookup(lookup).build());
                            }
                        }, Items.PURPLE_SHULKER_BOX);
                providerRegistered = true;
            } finally { registry.setLocked(true); }
        }
        for (var screen : new ModernSmokeClient.SlotHarness[]{inside, outside, lock}) screen.init(client, width, height);
        box.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("Shulker component preview"));
        box.set(net.minecraft.component.DataComponentTypes.RARITY, net.minecraft.util.Rarity.EPIC);
        try {
            box.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(
                    net.minecraft.nbt.StringNbtReader.parse("{TooltipStyle:'modern/forest',Monumenta:{Location:'forest',Tier:'artifact'}}")));
        } catch (Exception e) { throw new IllegalStateException(e); }
        box.set(net.minecraft.component.DataComponentTypes.CONTAINER, net.minecraft.component.type.ContainerComponent.fromStacks(
                java.util.List.of(new ItemStack(Items.DIAMOND,32), new ItemStack(Items.GOLD_INGOT,16))));
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
            context.drawTextWithShadow(font, "Shulker Box Tooltip 5.x / INSIDE", 15, 40, 0xffffff);
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
