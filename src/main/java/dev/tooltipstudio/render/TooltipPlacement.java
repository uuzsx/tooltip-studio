package dev.tooltipstudio.render;

/** Moves the whole visual tooltip along one screen axis while preserving the existing edge margin. */
public final class TooltipPlacement {
    private TooltipPlacement() {}

    public static int horizontal(int preferred, int visualSize, int screenSize, int mouseX, int offset, boolean cursorRelative) {
        // Use the original side decision, not the clamped x position: a very wide tooltip may straddle the cursor.
        boolean onLeft = (long) mouseX + 12 + visualSize > screenSize;
        return offset(preferred, visualSize, screenSize, cursorRelative && onLeft ? -offset : offset);
    }

    public static int offset(int preferred, int visualSize, int screenSize, int offset) {
        int normal = clamp(preferred, visualSize, screenSize);
        // Apply after normal placement and scaling, so offsets are in screen GUI pixels.
        // Clamp first as well: moving away from an edge should work immediately.
        return clamp((long) normal + offset, visualSize, screenSize);
    }

    private static int clamp(long position, int visualSize, int screenSize) {
        return (int) Math.max(4, Math.min((long) screenSize - visualSize - 4, position));
    }
}
