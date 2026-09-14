package dev.tooltipstudio.render;

import dev.tooltipstudio.compat.ShulkerCompatibility;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

/** Nested item tooltips restore the parent scope; no state survives a draw call. */
public final class TooltipRenderScope {
    public static final boolean SHULKER_LOADED = FabricLoader.getInstance().isModLoaded("shulkerboxtooltip");
    private static final ThreadLocal<State> ACTIVE = new ThreadLocal<>();
    public record State(ItemStack item, Object screen) {}
    private TooltipRenderScope() {}

    public static State enter(ItemStack item, Object screen) {
        State previous = ACTIVE.get();
        ACTIVE.set(new State(item, screen));
        return previous;
    }
    public static void restore(State previous) {
        if (previous == null) ACTIVE.remove();
        else ACTIVE.set(previous);
    }
    public static ItemStack item() {
        State state = ACTIVE.get();
        if (state == null) return null;
        return SHULKER_LOADED && state.screen != null
                ? ShulkerCompatibility.lockedItem(state.screen, state.item) : state.item;
    }
    public static boolean hasScreen() {
        State state = ACTIVE.get();
        return state != null && state.screen != null;
    }
}
