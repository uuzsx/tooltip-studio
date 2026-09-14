package dev.tooltipstudio.render;

import dev.tooltipstudio.config.Style;
import java.util.List;

/** Shared by nine-slice panels and three-slice separators. Caps are never resized. */
public final class Slices {
    private Slices() {}
    public record Segment(int source, int sourceSize, int target, int targetSize) {}

    public static List<Segment> axis(int sourceSize, int first, int last, int targetSize) {
        Style.require(first >= 0 && last >= 0 && sourceSize > first + last && targetSize >= first + last,
                "invalid slice dimensions");
        return List.of(new Segment(0, first, 0, first),
                new Segment(first, sourceSize - first - last, first, targetSize - first - last),
                new Segment(sourceSize - last, last, targetSize - last, last));
    }

    public static int anchorX(Style.Anchor a, int panelWidth, int spriteWidth) {
        return switch (a) {
            case TOP_LEFT, LEFT, BOTTOM_LEFT -> 0;
            case TOP, CENTER, BOTTOM -> (panelWidth - spriteWidth) / 2;
            case TOP_RIGHT, RIGHT, BOTTOM_RIGHT -> panelWidth - spriteWidth;
        };
    }
    public static int anchorY(Style.Anchor a, int panelHeight, int spriteHeight) {
        return switch (a) {
            case TOP_LEFT, TOP, TOP_RIGHT -> 0;
            case LEFT, CENTER, RIGHT -> (panelHeight - spriteHeight) / 2;
            case BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT -> panelHeight - spriteHeight;
        };
    }
}
