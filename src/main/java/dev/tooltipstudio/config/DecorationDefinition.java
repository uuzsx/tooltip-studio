package dev.tooltipstudio.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** An independent image or text overlay, with no base panel styling. */
public record DecorationDefinition(String texture, int textureWidth, int textureHeight,
                                   Style.Region region, Style.Anchor anchor, int x, int y, boolean foreground,
                                   String type, String text, String color, Boolean shadow, Boolean bold, Boolean italic,
                                   @SerializedName("x_scale") Double xScale, @SerializedName("y_scale") Double yScale,
                                   List<TextSegment> segments) implements DecorationSpec {
    public void validate() {
        validateDecoration();
        if (isText()) return;
        Style.require(texture != null && !texture.isBlank(), "decoration texture is required");
        Style.require(textureWidth > 0 && textureWidth <= 4096 && textureHeight > 0 && textureHeight <= 4096,
                "decoration texture dimensions must be 1..4096");
        Style.require(region != null && region.u() >= 0 && region.v() >= 0 && region.width() > 0 && region.height() > 0
                        && (long) region.u() + region.width() <= textureWidth && (long) region.v() + region.height() <= textureHeight,
                "decoration region lies outside the texture or has invalid dimensions");
    }
}
