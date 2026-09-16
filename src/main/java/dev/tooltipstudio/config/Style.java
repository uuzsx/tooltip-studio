package dev.tooltipstudio.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Atlas regions refer to one PNG; layout and whole-tooltip offsets use GUI pixels. */
public record Style(String texture, int textureWidth, int textureHeight,
                    Region background, Frame frame, Separator separator,
                    Insets padding, int minWidth, int maxWidth, List<Decoration> decorations,
                    int offsetX, int offsetY, String offsetXMode) {
    public record Region(int u, int v, int width, int height) {}
    public record Insets(int left, int top, int right, int bottom) {}
    public record Frame(Region region, int left, int top, int right, int bottom) {}
    public record Separator(boolean enabled, Region region, int leftCap, int rightCap,
                            int inset, int marginTop, int marginBottom) {}
    public enum Anchor {
        TOP_LEFT, TOP, TOP_RIGHT, LEFT, CENTER, RIGHT, BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT,
        SEPARATOR_LEFT, SEPARATOR_CENTER, SEPARATOR_RIGHT;

        public boolean separator() { return this == SEPARATOR_LEFT || this == SEPARATOR_CENTER || this == SEPARATOR_RIGHT; }
    }
    public record Decoration(Region region, Anchor anchor, int x, int y, boolean foreground,
                             String type, String text, String color, Boolean shadow, Boolean bold, Boolean italic,
                             @SerializedName("x_scale") Double xScale, @SerializedName("y_scale") Double yScale) implements DecorationSpec {}

    public boolean cursorRelativeOffset() { return offsetXMode == null || "cursor".equals(offsetXMode); }

    public void validate() {
        require(texture != null && !texture.isBlank(), "texture is required");
        require(textureWidth > 0 && textureWidth <= 4096 && textureHeight > 0 && textureHeight <= 4096,
                "texture dimensions must be 1..4096");
        region(background, "background");
        require(frame != null, "frame is required");
        region(frame.region, "frame.region");
        require(frame.left >= 0 && frame.right >= 0 && frame.top >= 0 && frame.bottom >= 0,
                "frame borders must be nonnegative");
        require(frame.left + frame.right < frame.region.width && frame.top + frame.bottom < frame.region.height,
                "frame must have a nonempty stretchable center");
        require(padding != null, "padding is required");
        require(padding.left >= frame.left && padding.right >= frame.right
                        && padding.top >= frame.top && padding.bottom >= frame.bottom,
                "padding must keep text inside frame borders");
        require(padding.left <= 128 && padding.right <= 128 && padding.top <= 128 && padding.bottom <= 128,
                "padding must be at most 128");
        require(minWidth > 0 && maxWidth >= minWidth && maxWidth <= 2048, "invalid minWidth/maxWidth");
        require(Math.abs((long) offsetX) <= 4096 && Math.abs((long) offsetY) <= 4096,
                "offsetX/offsetY must be -4096..4096");
        require(offsetXMode == null || "cursor".equals(offsetXMode) || "screen".equals(offsetXMode),
                "offsetXMode must be cursor or screen");
        if (separator != null && separator.enabled) {
            region(separator.region, "separator.region");
            require(separator.leftCap >= 0 && separator.rightCap >= 0
                            && separator.leftCap + separator.rightCap < separator.region.width,
                    "separator must have a nonempty stretchable center");
            require(separator.inset >= 0 && separator.inset <= 128 && separator.marginTop >= 0
                    && separator.marginBottom >= 0 && separator.marginTop <= 128 && separator.marginBottom <= 128,
                    "invalid separator spacing");
            require(maxWidth >= separator.leftCap + separator.rightCap + 2 * separator.inset + 1,
                    "maxWidth is too small for fixed separator caps");
        }
        require(decorations != null && decorations.size() <= 64, "decorations must be a list of at most 64 entries");
        for (Decoration decoration : decorations) {
            require(decoration != null, "decoration is required");
            decoration.validateDecoration();
            if (!decoration.isText()) region(decoration.region, "decoration.region");
        }
    }

    private void region(Region r, String name) {
        require(r != null && r.u >= 0 && r.v >= 0 && r.width > 0 && r.height > 0
                        && (long) r.u + r.width <= textureWidth && (long) r.v + r.height <= textureHeight,
                name + " lies outside the texture or has invalid dimensions");
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
