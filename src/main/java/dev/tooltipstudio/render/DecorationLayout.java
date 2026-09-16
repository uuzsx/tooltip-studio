package dev.tooltipstudio.render;

import dev.tooltipstudio.config.DecorationSpec;

/** Layout in unscaled tooltip GUI coordinates. The separator rectangle is the actual drawn line. */
public final class DecorationLayout {
    private DecorationLayout() {}
    public record Box(float x, float y, float width, float height) {}
    public record Separator(float x, float y, float width, float height) {}

    public static Box place(DecorationSpec d, int naturalWidth, int naturalHeight, int panelWidth, int panelHeight,
                            Separator separator) {
        float width = naturalWidth * d.scaleX(), height = naturalHeight * d.scaleY();
        float x, y;
        if (d.anchor().separator()) {
            if (separator == null) return null; // Title-only and disabled-separator tooltips have no such anchor.
            x = switch (d.anchor()) {
                case SEPARATOR_LEFT -> separator.x();
                case SEPARATOR_RIGHT -> separator.x() + separator.width() - width;
                default -> separator.x() + (separator.width() - width) / 2f;
            };
            y = separator.y() + (separator.height() - height) / 2f;
        } else {
            x = switch (d.anchor()) {
                case TOP_LEFT, LEFT, BOTTOM_LEFT -> 0;
                case TOP, CENTER, BOTTOM -> (int) ((panelWidth - width) / 2f);
                default -> panelWidth - width;
            };
            y = switch (d.anchor()) {
                case TOP_LEFT, TOP, TOP_RIGHT -> 0;
                case LEFT, CENTER, RIGHT -> (int) ((panelHeight - height) / 2f);
                default -> panelHeight - height;
            };
        }
        return new Box(x + d.x(), y + d.y(), width, height);
    }
}
