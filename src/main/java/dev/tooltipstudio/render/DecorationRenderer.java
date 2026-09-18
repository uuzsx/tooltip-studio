package dev.tooltipstudio.render;

import dev.tooltipstudio.config.ConfigManager.LoadedStyle;
import dev.tooltipstudio.config.DecorationSpec;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Resolves image and text sizes once, so clipping bounds and drawing use identical geometry. */
final class DecorationRenderer {
    private DecorationRenderer() {}
    record Entry(DecorationSpec definition, Identifier texture, int textureWidth, int textureHeight,
                 List<Text> lines, int textInset, DecorationLayout.Box box) {}

    static List<Entry> prepare(LoadedStyle loaded, TextRenderer font, int width, int height, DecorationLayout.Separator separator) {
        List<Entry> result = new ArrayList<>();
        for (var d : loaded.style().decorations())
            add(result, d, loaded.texture(), loaded.style().textureWidth(), loaded.style().textureHeight(), font, width, height, separator);
        for (var overlay : loaded.decorations()) {
            var d = overlay.definition();
            add(result, d, overlay.texture(), d.textureWidth(), d.textureHeight(), font, width, height, separator);
        }
        return result;
    }

    private static void add(List<Entry> result, DecorationSpec d, Identifier texture, int tw, int th,
                            TextRenderer font, int panelWidth, int panelHeight, DecorationLayout.Separator separator) {
        if (d.anchor().separator() && separator == null) return;
        List<Text> lines = new ArrayList<>();
        int naturalWidth, naturalHeight, inset = 0;
        if (d.isText()) {
            var content = DecorationText.resolve(d);
            lines.addAll(content.lines());
            inset = content.italic() ? 2 : 0;
            int shadow = d.hasShadow() ? 1 : 0;
            naturalWidth = Math.max(1, lines.stream().mapToInt(font::getWidth).max().orElse(0)) + 2 * inset + shadow;
            naturalHeight = lines.size() * font.fontHeight + shadow;
        } else {
            naturalWidth = d.region().width(); naturalHeight = d.region().height();
        }
        var box = DecorationLayout.place(d, naturalWidth, naturalHeight, panelWidth, panelHeight, separator);
        result.add(new Entry(d, texture, tw, th, List.copyOf(lines), inset, box));
    }

    static void draw(DrawContext context, TextRenderer font, List<Entry> entries, boolean foreground) {
        for (var entry : entries) {
            var d = entry.definition();
            if (d.foreground() != foreground) continue;
            var matrices = context.getMatrices();
            matrices.push();
            try {
                matrices.translate(entry.box().x(), entry.box().y(), 0);
                matrices.scale(d.scaleX(), d.scaleY(), 1);
                if (d.isText()) {
                    for (int i = 0; i < entry.lines().size(); i++)
                        context.drawText(font, entry.lines().get(i), entry.textInset(), i * font.fontHeight, d.textColor(), d.hasShadow());
                } else {
                    var r = d.region();
                    context.drawTexture(entry.texture(), 0, 0, r.width(), r.height(), (float) r.u(), (float) r.v(),
                            r.width(), r.height(), entry.textureWidth(), entry.textureHeight());
                }
            } finally {
                context.draw();
                matrices.pop();
            }
        }
    }
}
