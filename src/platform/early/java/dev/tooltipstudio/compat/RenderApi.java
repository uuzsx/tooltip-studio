package dev.tooltipstudio.compat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.util.Identifier;

public final class RenderApi {
    private RenderApi() {}
    public static int height(TooltipComponent component, TextRenderer font) { return component.getHeight(); }
    public static void text(TooltipComponent component, TextRenderer font, int x, int y, DrawContext context) {
        component.drawText(font, x, y, context.getMatrices().peek().getPositionMatrix(), context.getVertexConsumers());
    }
    public static void items(TooltipComponent component, TextRenderer font, int x, int y, int width, int height, DrawContext context) {
        component.drawItems(font, x, y, context);
    }
    public static void texture(DrawContext context, Identifier id, int x, int y, int width, int height,
                               float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        context.drawTexture(id, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }
}
