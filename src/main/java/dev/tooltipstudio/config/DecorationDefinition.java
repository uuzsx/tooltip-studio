package dev.tooltipstudio.config;

/** An independent overlay atlas; it never supplies a tooltip background, frame or separator. */
public record DecorationDefinition(String texture, int textureWidth, int textureHeight,
                                   Style.Region region, Style.Anchor anchor, int x, int y, boolean foreground) {
    public void validate() {
        Style.require(texture != null && !texture.isBlank(), "decoration texture is required");
        Style.require(textureWidth > 0 && textureWidth <= 4096 && textureHeight > 0 && textureHeight <= 4096,
                "decoration texture dimensions must be 1..4096");
        Style.require(region != null && region.u() >= 0 && region.v() >= 0 && region.width() > 0 && region.height() > 0
                        && (long) region.u() + region.width() <= textureWidth && (long) region.v() + region.height() <= textureHeight,
                "decoration region lies outside the texture or has invalid dimensions");
        Style.require(anchor != null, "decoration anchor is required");
        Style.require(Math.abs((long) x) <= 256 && Math.abs((long) y) <= 256, "decoration offsets must be -256..256");
    }

    public Style.Decoration placement() { return new Style.Decoration(region, anchor, x, y, foreground); }
}
