package dev.tooltipstudio.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.tooltipstudio.render.TooltipRenderScope;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected Slot focusedSlot;

    // Keep other mods' redirects intact; carry item context to DrawContext only.
    // WrapMethod restores the previous scope even on cancellation or exceptions.
    @WrapMethod(method = "drawMouseoverTooltip")
    private void tooltipstudio$scopeSlot(DrawContext context, int x, int y, Operation<Void> original) {
        var previous = TooltipRenderScope.enter(focusedSlot == null ? ItemStack.EMPTY : focusedSlot.getStack(), this);
        try { original.call(context, x, y); }
        finally { TooltipRenderScope.restore(previous); }
    }
}
