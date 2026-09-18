package dev.tooltipstudio.render;

import dev.tooltipstudio.config.DecorationSpec;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Builds each line as one styled text so Minecraft measures and draws the same contiguous runs. */
public final class DecorationText {
    private DecorationText() {}
    public record Content(List<Text> lines, boolean italic) {}

    public static Content resolve(DecorationSpec definition) {
        Style base = Style.EMPTY.withColor(definition.textColor() & 0xffffff)
                .withBold(definition.isBold()).withItalic(definition.isItalic());
        List<Text> lines = new ArrayList<>();
        MutableText line = Text.empty().setStyle(base);
        String combined = definition.plainText();
        int position = 0;
        boolean italic = false;
        for (var segment : definition.textSegments()) {
            Style style = base;
            if (segment.color() != null) style = style.withColor(Integer.parseInt(segment.color().substring(1), 16));
            if (segment.bold() != null) style = style.withBold(segment.bold());
            if (segment.italic() != null) style = style.withItalic(segment.italic());
            StringBuilder part = new StringBuilder();
            for (int i = 0; i < segment.text().length(); i++, position++) {
                char c = segment.text().charAt(i);
                // A Windows line ending remains one break, even when split across two segments.
                if (c == '\r' && position + 1 < combined.length() && combined.charAt(position + 1) == '\n') continue;
                if (c == '\n') {
                    append(line, part, style);
                    lines.add(line);
                    line = Text.empty().setStyle(base);
                } else {
                    part.append(c);
                    italic |= style.isItalic();
                }
            }
            append(line, part, style);
        }
        lines.add(line);
        return new Content(List.copyOf(lines), italic);
    }

    private static void append(MutableText line, StringBuilder part, Style style) {
        if (!part.isEmpty()) {
            line.append(Text.literal(part.toString()).setStyle(style));
            part.setLength(0);
        }
    }
}
