package dev.tooltipstudio.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** An independent image or text overlay, with no base panel styling. */
public record DecorationDefinition(String texture, int textureWidth, int textureHeight,
                                   Style.Region region, Style.Anchor anchor, int x, int y, boolean foreground,
                                   String type, String text, String color, Boolean shadow, Boolean bold, Boolean italic,
                                   @SerializedName("x_scale") Double xScale, @SerializedName("y_scale") Double yScale,
                                   List<TextSegment> segments, DecorationAnimation animation) implements DecorationSpec {
    public void validate() {
        validateDecoration();
        if (isText()) return;
        Style.require(texture != null && !texture.isBlank(), "decoration texture is required");
        validateImage(textureWidth, textureHeight);
    }
}
