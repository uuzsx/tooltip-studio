package dev.tooltipstudio.config;

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
            Style.require(text() != null && !text().isBlank() && text().length() <= 1024,
                    "text decoration requires 1..1024 characters");
            Style.require(text().split("\\n", -1).length <= 16, "text decoration supports at most 16 lines");
            Style.require(color() == null || color().matches("#[0-9a-fA-F]{6}"), "text color must be #RRGGBB");
        } else {
            Style.require(text() == null, "set type to text when supplying decoration text");
        }
    }

    private static void validateScale(Double value, String name) {
        Style.require(value == null || Double.isFinite(value) && value >= 0.0625 && value <= 16,
                name + " must be a finite number in 0.0625..16 (omitting it uses 1)");
    }
}
