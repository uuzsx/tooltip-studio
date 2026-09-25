package dev.tooltipstudio.compat;
import dev.tooltipstudio.render.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class RenderApi {
    private RenderApi() {}
    public static TooltipCanvas canvas(GuiGraphics graphics, Font font) { return new Canvas(graphics, font); }
    public static ClientTooltipComponent title(TitleData data) { return new Title(data); }
    private static final class Title extends TitleComponent {
        Title(TitleData data) { super(data); }
        @Override public void extractText(GuiGraphics graphics, Font font, int x, int y) {
            int width = getWidth(font);
            for (var line : lines(font)) {
                graphics.text(font,line,x+(width-font.width(line))/2,y,-1,true);
                y += font.lineHeight + 1;
            }
        }
    }
    private record Canvas(GuiGraphics graphics, Font font) implements TooltipCanvas {
        public int width() { return graphics.guiWidth(); }
        public int height() { return graphics.guiHeight(); }
        public void begin() {}
        public void end() {}
        public void push() { graphics.pose().pushMatrix(); }
        public void pop() { graphics.pose().popMatrix(); }
        public void translate(float x,float y,float z) { if (z != 0) graphics.nextStratum(); graphics.pose().translate(x,y); }
        public void scale(float x,float y,float z) { graphics.pose().scale(x,y); }
        public void flush() {}
        public int componentHeight(ClientTooltipComponent c) { return c.getHeight(font); }
        public void componentText(ClientTooltipComponent c,int x,int y) { c.extractText(graphics,font,x,y); }
        public void componentImage(ClientTooltipComponent c,int x,int y,int w,int h) { c.extractImage(font,x,y,w,h,graphics); }
        public void text(Component text,int x,int y,int color,boolean shadow) { graphics.text(font,text,x,y,color,shadow); }
        public void texture(ResourceLocation id,int x,int y,int w,int h,float u,float v,int sw,int sh,int tw,int th) { graphics.blit(RenderPipelines.GUI_TEXTURED,id,x,y,u,v,w,h,sw,sh,tw,th); }
    }
}
