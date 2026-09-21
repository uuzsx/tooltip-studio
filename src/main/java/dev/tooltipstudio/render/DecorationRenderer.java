package dev.tooltipstudio.render;

import dev.tooltipstudio.config.ConfigManager.LoadedStyle;
import dev.tooltipstudio.config.DecorationSpec;
import dev.tooltipstudio.config.Style;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Resolves image and text sizes once, so clipping bounds and drawing use identical geometry. */
final class DecorationRenderer {
    private DecorationRenderer() {}
    private static final long ANIMATION_START = System.nanoTime();
    record Entry(DecorationSpec definition, Identifier texture, int textureWidth, int textureHeight,
                 List<Text> lines, int textInset, DecorationLayout.Box box, Style.Region region) {}

    static List<Entry> prepare(LoadedStyle loaded, TextRenderer font, int width, int height, DecorationLayout.Separator separator) {
        List<Entry> result = new ArrayList<>();
        long elapsed = (System.nanoTime() - ANIMATION_START) / 1_000_000L;
        for (int i = 0; i < loaded.style().decorations().size(); i++) {
            var d = loaded.style().decorations().get(i);
            add(result, d, loaded.inlineTextures().getOrDefault(i, loaded.texture()),
                    d.imageWidth(loaded.style()), d.imageHeight(loaded.style()), font, width, height, separator, elapsed);
        }
        for (var overlay : loaded.decorations()) {
            var d = overlay.definition();
            add(result, d, overlay.texture(), d.textureWidth(), d.textureHeight(), font, width, height, separator, elapsed);
        }
        return result;
    }

    private static void add(List<Entry> result, DecorationSpec d, Identifier texture, int tw, int th,
                            TextRenderer font, int panelWidth, int panelHeight, DecorationLayout.Separator separator, long elapsed) {
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
        var region = d.animation() == null ? d.region() : d.animation().regionAt(d.region(), elapsed);
        result.add(new Entry(d, texture, tw, th, List.copyOf(lines), inset, box, region));
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
                    var r = entry.region();
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
