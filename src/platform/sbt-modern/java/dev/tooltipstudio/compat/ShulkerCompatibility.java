package dev.tooltipstudio.compat;

import com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltip;
import com.misterpemodder.shulkerboxtooltip.impl.config.Configuration.PreviewPosition;
import com.misterpemodder.shulkerboxtooltip.impl.tooltip.PositionAwareClientTooltipComponent;
import com.misterpemodder.shulkerboxtooltip.impl.hook.GuiGraphicsExtensions;
import dev.tooltipstudio.config.ConfigManager;
import dev.tooltipstudio.render.TooltipRenderScope;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.lang.reflect.Field;

/** Optional bridge for Shulker Box Tooltip 5.x. Not loaded when the mod is absent. */
public final class ShulkerCompatibility {
    private static final Field LOCKED_SLOT = findLockedSlot();
    private ShulkerCompatibility() {}

    private static Field findLockedSlot() {
        try {
            // Introduced by SBT's mixin, not a mapped Minecraft field. Resolve once.
            Field field = HandledScreen.class.getDeclaredField("mouseLockSlot");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            ConfigManager.LOGGER.warn("Shulker Box Tooltip lock field unavailable; using hovered item for style selection");
            return null;
        }
    }

    public static ItemStack lockedItem(Object screen, ItemStack hovered) {
        if (LOCKED_SLOT != null && screen instanceof HandledScreen<?>) {
            try {
                Slot locked = (Slot) LOCKED_SLOT.get(screen);
                if (locked != null) return locked.getStack();
            } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        }
        return hovered;
    }

    public static boolean draw(TooltipComponent component, TextRenderer font, int x, int y, DrawContext context,
                               float originX, float originY, float scale, int panelWidth, int panelHeight,
                               int mouseX, int mouseY) {
        if (!(component instanceof PositionAwareClientTooltipComponent positioned)) return false;
        // Locked placement coordinates differ from the live mouse used for preview hit-testing.
        if (TooltipRenderScope.hasScreen() && context instanceof GuiGraphicsExtensions positions) {
            mouseX = positions.getMouseX();
            mouseY = positions.getMouseY();
        }
        if (ShulkerBoxTooltip.config.preview.position == PreviewPosition.INSIDE) {
            positioned.drawItemsWithTooltipPosition(font, x, y, context, 0, panelHeight,
                    Math.round((mouseX - originX) / scale), Math.round((mouseY - originY) / scale));
        } else {
            // Outside previews use screen coordinates and handle their own hit-testing.
            var matrices = context.getMatrices();
            matrices.push();
            try {
                matrices.scale(1 / scale, 1 / scale, 1);
                matrices.translate(-originX, -originY, 0);
                positioned.drawItemsWithTooltipPosition(font, Math.round(originX + x * scale),
                        Math.round(originY + y * scale), context, Math.round(originY),
                        Math.round(originY + panelHeight * scale), mouseX, mouseY);
            } finally { matrices.pop(); }
        }
        return true;
    }
}
