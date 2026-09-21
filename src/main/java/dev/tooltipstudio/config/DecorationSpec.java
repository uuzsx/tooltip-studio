package dev.tooltipstudio.config;

import java.util.List;

/** Shared options for decorations inside a base style and independently matched decorations. */
public interface DecorationSpec {
    Style.Region region();
    Style.Anchor anchor();
    int x();
    int y();
    boolean foreground();
    String type();
    String text();
    String color();
    Boolean shadow();
    Boolean bold();
    Boolean italic();
    Double xScale();
    Double yScale();
    List<TextSegment> segments();
    DecorationAnimation animation();

    record TextSegment(String text, String color, Boolean bold, Boolean italic) {}

    /** Nullable new fields keep legacy JSON serialization (and preset fingerprints) unchanged. */
    default List<TextSegment> textSegments() {
        return segments() == null ? List.of(new TextSegment(text(), null, null, null)) : segments();
    }

    default String plainText() {
        if (segments() == null) return text();
        StringBuilder joined = new StringBuilder();
        for (TextSegment segment : segments()) joined.append(segment.text());
        return joined.toString();
    }

    default boolean isText() { return "text".equals(type()); }
    default float scaleX() { return xScale() == null ? 1f : xScale().floatValue(); }
    default float scaleY() { return yScale() == null ? 1f : yScale().floatValue(); }
    default boolean hasShadow() { return shadow() == null || shadow(); }
    default boolean isBold() { return Boolean.TRUE.equals(bold()); }
    default boolean isItalic() { return Boolean.TRUE.equals(italic()); }
    default int textColor() { return color() == null ? 0xffffffff : 0xff000000 | Integer.parseInt(color().substring(1), 16); }

    default void validateDecoration() {
        Style.require(type() == null || "texture".equals(type()) || isText(), "decoration type must be texture or text");
        Style.require(anchor() != null, "decoration anchor is required");
        Style.require(Math.abs((long) x()) <= 256 && Math.abs((long) y()) <= 256, "decoration offsets must be -256..256");
        validateScale(xScale(), "x_scale"); validateScale(yScale(), "y_scale");
        if (isText()) {
            Style.require(animation() == null, "animation is only supported for texture decorations");
            Style.require((text() != null) != (segments() != null), "text decoration requires exactly one of text or segments");
            validateColor(color(), "text color");
            if (segments() != null) {
                Style.require(!segments().isEmpty() && segments().size() <= 64, "segments must contain 1..64 entries");
                int total = 0;
                for (int i = 0; i < segments().size(); i++) {
                    TextSegment segment = segments().get(i);
                    Style.require(segment != null && segment.text() != null, "segments[" + i + "] requires text");
                    total += segment.text().length();
                    Style.require(total <= 1024, "text decoration requires at most 1024 characters across all segments");
                    validateColor(segment.color(), "segments[" + i + "].color");
                }
            }
            String joined = plainText();
            Style.require(!joined.isBlank() && joined.length() <= 1024,
                    "text decoration requires 1..1024 characters");
            Style.require(joined.split("\\n", -1).length <= 16, "text decoration supports at most 16 lines");
        } else {
            Style.require(text() == null && segments() == null, "set type to text when supplying decoration text or segments");
        }
    }

    default void validateImage(int textureWidth, int textureHeight) {
        Style.require(textureWidth > 0 && textureWidth <= 4096 && textureHeight > 0 && textureHeight <= 4096,
                "decoration texture dimensions must be 1..4096");
        var r = region();
        Style.require(r != null && r.u() >= 0 && r.v() >= 0 && r.width() > 0 && r.height() > 0
                        && (long) r.u() + r.width() <= textureWidth && (long) r.v() + r.height() <= textureHeight,
                "decoration region lies outside the texture or has invalid dimensions");
        if (animation() != null) animation().validate(r, textureWidth, textureHeight);
    }

    private static void validateColor(String color, String name) {
        Style.require(color == null || color.matches("#[0-9a-fA-F]{6}"), name + " must be #RRGGBB");
    }

    private static void validateScale(Double value, String name) {
        Style.require(value == null || Double.isFinite(value) && value >= 0.0625 && value <= 16,
                name + " must be a finite number in 0.0625..16 (omitting it uses 1)");
    }
}
