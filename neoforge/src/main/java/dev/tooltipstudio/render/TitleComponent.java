package dev.tooltipstudio.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

public abstract class TitleComponent implements ClientTooltipComponent {
    private final TitleData data;
    protected TitleComponent(TitleData data) { this.data = data; }
    public List<FormattedCharSequence> lines(Font font) { return font.split(data.text(), data.wrapWidth()); }
    @Override public int getWidth(Font font) { return lines(font).stream().mapToInt(font::width).max().orElse(0); }
    public int getHeight() { return getHeight(Minecraft.getInstance().font); }
    public int getHeight(Font font) { return Math.max(1, lines(font).size()) * (font.lineHeight + 1); }
}
