package dev.tooltipstudio.compat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
/** No optional SBT bridge is configured for 1.21.2; ordinary component rendering remains enabled. */
public final class ShulkerCompatibility {
    public static ItemStack lockedItem(Object screen, ItemStack hovered) { return hovered; }
    public static boolean draw(TooltipComponent component, TextRenderer font, int x, int y, DrawContext context,
                               float originX, float originY, float scale, int panelWidth, int panelHeight, int mouseX, int mouseY) { return false; }
}
