package dev.tooltipstudio.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.tooltipstudio.TooltipStudioClient;
import dev.tooltipstudio.render.StyledTooltipRenderer;
import dev.tooltipstudio.render.TooltipRenderScope;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.Optional;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @WrapMethod(method = "drawItemTooltip")
    private void tooltipstudio$scopeItem(TextRenderer font, ItemStack stack, int x, int y, Operation<Void> original) {
        var previous = TooltipRenderScope.enter(stack, null);
        try { original.call(font, stack, x, y); }
        finally { TooltipRenderScope.restore(previous); }
    }

    // Inventory mods have now supplied the final text, data and locked coordinates.
    @WrapMethod(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V")
    private void tooltipstudio$drawScoped(TextRenderer font, List<Text> text, Optional<TooltipData> data,
                                         int x, int y, Operation<Void> original) {
        ItemStack item = TooltipRenderScope.item();
        var style = item == null ? null : TooltipStudioClient.CONFIG.select(item);
        if (style == null) original.call(font, text, data, x, y);
        else StyledTooltipRenderer.draw((DrawContext) (Object) this, font, text, data, x, y, style);
    }
}
