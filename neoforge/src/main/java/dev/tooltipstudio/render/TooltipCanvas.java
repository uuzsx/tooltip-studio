package dev.tooltipstudio.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Small boundary between 1.21 immediate rendering and 26.x GUI extraction. */
public interface TooltipCanvas {
    Font font();
    int width();
    int height();
    void begin();
    void end();
    void push();
    void pop();
    void translate(float x, float y, float z);
    void scale(float x, float y, float z);
    void flush();
    int componentHeight(ClientTooltipComponent component);
    void componentText(ClientTooltipComponent component, int x, int y);
    void componentImage(ClientTooltipComponent component, int x, int y, int width, int height);
    void text(Component text, int x, int y, int color, boolean shadow);
    void texture(ResourceLocation id, int x, int y, int width, int height, float u, float v,
                 int sourceWidth, int sourceHeight, int textureWidth, int textureHeight);
}
