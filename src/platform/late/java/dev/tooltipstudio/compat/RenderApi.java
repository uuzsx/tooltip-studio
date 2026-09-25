package dev.tooltipstudio.compat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.util.Identifier;

public final class RenderApi {
    private RenderApi() {}
    public static int height(TooltipComponent component, TextRenderer font) { return component.getHeight(font); }
    public static void text(TooltipComponent component, TextRenderer font, int x, int y, DrawContext context) {
        component.drawText(font, x, y, context.getMatrices().peek().getPositionMatrix(), ((dev.tooltipstudio.mixin.DrawContextAccessor) context).tooltipstudio$vertices());
    }
    public static void items(TooltipComponent component, TextRenderer font, int x, int y, int width, int height, DrawContext context) {
        component.drawItems(font, x, y, width, height, context);
    }
    public static void texture(DrawContext context, Identifier id, int x, int y, int width, int height,
                               float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }
}
