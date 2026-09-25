package dev.tooltipstudio.compat;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.tooltipstudio.render.*;
import dev.tooltipstudio.mixin.GuiGraphicsAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class RenderApi {
    private RenderApi() {}
    public static TooltipCanvas canvas(GuiGraphics graphics, Font font) { return new Canvas(graphics, font); }
    public static ClientTooltipComponent title(TitleData data) { return new Title(data); }
    private static final class Title extends TitleComponent {
        Title(TitleData data) { super(data); }
        @Override public void renderText(Font font, int x, int y, Matrix4f pose, MultiBufferSource.BufferSource buffers) {
            int width = getWidth(font);
            for (var line : lines(font)) {
                font.drawInBatch(line, x + (width - font.width(line)) / 2f, y, -1, true, pose, buffers, Font.DisplayMode.NORMAL, 0, 15728880);
                y += font.lineHeight + 1;
            }
        }
    }
    private static final class Canvas implements TooltipCanvas {
        private final GuiGraphics graphics;
        private final Font font;
        private float[] color;
        Canvas(GuiGraphics graphics, Font font) { this.graphics = graphics; this.font = font; }
        public Font font() { return font; }
        public int width() { return graphics.guiWidth(); }
        public int height() { return graphics.guiHeight(); }
        public void begin() { graphics.flush(); color = RenderSystem.getShaderColor().clone(); RenderSystem.setShaderColor(1,1,1,1); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); }
        public void end() { graphics.flush(); RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]); RenderSystem.disableBlend(); }
        public void push() { graphics.pose().pushPose(); }
        public void pop() { graphics.pose().popPose(); }
        public void translate(float x,float y,float z) { graphics.pose().translate(x,y,z); }
        public void scale(float x,float y,float z) { graphics.pose().scale(x,y,z); }
        public void flush() { graphics.flush(); }
        public int componentHeight(ClientTooltipComponent c) { return c.getHeight(); }
        public void componentText(ClientTooltipComponent c,int x,int y) { c.renderText(font,x,y,graphics.pose().last().pose(),((GuiGraphicsAccessor)graphics).tooltipstudio$buffers()); }
        public void componentImage(ClientTooltipComponent c,int x,int y,int w,int h) { c.renderImage(font,x,y,graphics); }
        public void text(Component text,int x,int y,int color,boolean shadow) { graphics.drawString(font,text,x,y,color,shadow); }
        public void texture(ResourceLocation id,int x,int y,int w,int h,float u,float v,int sw,int sh,int tw,int th) { graphics.blit(id,x,y,w,h,u,v,sw,sh,tw,th); }
    }
}
