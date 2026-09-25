package dev.tooltipstudio.render;

import dev.tooltipstudio.compat.VersionApi;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.tooltipstudio.compat.ShulkerCompatibility;
import dev.tooltipstudio.compat.RenderApi;
import dev.tooltipstudio.config.ConfigManager.LoadedStyle;
import dev.tooltipstudio.config.Style;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StyledTooltipRenderer {
    private StyledTooltipRenderer() {}
    private record Row(TooltipComponent component, boolean title, int y) {}

    public static void draw(DrawContext context, TextRenderer textRenderer, List<Text> lines,
                            Optional<?> data, int mouseX, int mouseY, LoadedStyle loaded) {
        if (lines.isEmpty()) return;
        Style style = loaded.style();
        Style.Insets padding = style.padding();
        Style.Separator separator = style.separator();
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int capMinimum = separator != null && separator.enabled()
                ? separator.leftCap() + separator.rightCap() + separator.inset() * 2 + 1 : 1;
        int wrapWidth = Math.max(capMinimum, Math.min(style.maxWidth(), screenWidth - padding.left() - padding.right() - 24));
        List<TooltipComponent> titles = new ArrayList<>();
        for (OrderedText text : textRenderer.wrapLines(lines.get(0), wrapWidth)) titles.add(TooltipComponent.of(text));
        if (titles.isEmpty()) titles.add(TooltipComponent.of(lines.get(0).asOrderedText()));
        List<TooltipComponent> body = new ArrayList<>();
        data.ifPresent(value -> body.add(VersionApi.tooltipComponent(value)));
        for (int i = 1; i < lines.size(); i++) {
            List<OrderedText> wrapped = textRenderer.wrapLines(lines.get(i), wrapWidth);
            if (wrapped.isEmpty()) body.add(TooltipComponent.of(lines.get(i).asOrderedText()));
            else for (OrderedText text : wrapped) body.add(TooltipComponent.of(text));
        }
        int contentWidth = Math.max(capMinimum, Math.min(style.minWidth(), wrapWidth));
        for (TooltipComponent component : titles) contentWidth = Math.max(contentWidth, component.getWidth(textRenderer));
        for (TooltipComponent component : body) contentWidth = Math.max(contentWidth, component.getWidth(textRenderer));
        List<Row> rows = new ArrayList<>();
        int y = padding.top();
        for (TooltipComponent component : titles) {
            rows.add(new Row(component, true, y));
            y += RenderApi.height(component, textRenderer);
        }
        int separatorY = -1;
        if (!body.isEmpty()) {
            if (separator != null && separator.enabled()) {
                y += separator.marginTop();
                separatorY = y;
                y += separator.region().height() + separator.marginBottom();
            } else y += 2;
        }
        for (TooltipComponent component : body) {
            rows.add(new Row(component, false, y));
            y += RenderApi.height(component, textRenderer);
        }
        int width = contentWidth + padding.left() + padding.right();
        int height = y + padding.bottom();
        var line = separatorY < 0 ? null : new DecorationLayout.Separator(padding.left() + separator.inset(),
                separatorY, contentWidth - separator.inset() * 2, separator.region().height());
        var decorations = DecorationRenderer.prepare(loaded, textRenderer, width, height, line);
        float left = 0, top = 0, right = width, bottom = height;
        for (var decoration : decorations) {
            var box = decoration.box();
            left = Math.min(left, box.x()); top = Math.min(top, box.y());
            right = Math.max(right, box.x() + box.width()); bottom = Math.max(bottom, box.y() + box.height());
        }
        // A tooltip that cannot fit (e.g. a tall bundle or many lore lines) scales as a whole.
        // Text wrapping handles the normal case. Decorations are included in the screen bounds.
        float scale = Math.min(1f, Math.min(Math.max(1, screenWidth - 8) / (float) (right - left),
                Math.max(1, screenHeight - 8) / (float) (bottom - top)));
        int visualWidth = (int) Math.ceil((right - left) * scale);
        int visualHeight = (int) Math.ceil((bottom - top) * scale);
        var position = HoveredTooltipPositioner.INSTANCE.getPosition(screenWidth, screenHeight, mouseX, mouseY, visualWidth, visualHeight);
        int px = TooltipPlacement.horizontal(position.x(), visualWidth, screenWidth, mouseX, style.offsetX(), style.cursorRelativeOffset());
        int py = TooltipPlacement.offset(position.y(), visualHeight, screenHeight, style.offsetY());
        var matrices = context.getMatrices();
        float[] color = RenderSystem.getShaderColor().clone();
        context.draw();
        matrices.push();
        try {
            matrices.translate(px - left * scale, py - top * scale, 400);
            matrices.scale(scale, scale, 1);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            sprite(context, loaded, style.background(), 0, 0, width, height);
            frame(context, loaded, width, height);
            DecorationRenderer.draw(context, textRenderer, decorations, false);
            if (separatorY >= 0) {
                Style.Region r = separator.region();
                int lineWidth = contentWidth - 2 * separator.inset();
                for (Slices.Segment s : Slices.axis(r.width(), separator.leftCap(), separator.rightCap(), lineWidth)) {
                    sprite(context, loaded, new Style.Region(r.u() + s.source(), r.v(), s.sourceSize(), r.height()),
                            padding.left() + separator.inset() + s.target(), separatorY, s.targetSize(), r.height());
                }
            }
            matrices.translate(0, 0, 1);
            // Preserve both vanilla tooltip-component passes, including bundle/item previews.
            for (Row row : rows) {
                int x = padding.left() + (row.title ? (contentWidth - row.component.getWidth(textRenderer)) / 2 : 0);
                RenderApi.text(row.component, textRenderer, x, row.y, context);
            }
            context.draw();
            for (Row row : rows) {
                int x = padding.left() + (row.title ? (contentWidth - row.component.getWidth(textRenderer)) / 2 : 0);
                if (!TooltipRenderScope.SHULKER_LOADED || !ShulkerCompatibility.draw(row.component, textRenderer,
                        x, row.y, context, px - left * scale, py - top * scale, scale, contentWidth, height, mouseX, mouseY))
                    RenderApi.items(row.component, textRenderer, x, row.y, contentWidth, height, context);
            }
            context.draw();
            matrices.translate(0, 0, 1);
            RenderSystem.enableBlend();
            DecorationRenderer.draw(context, textRenderer, decorations, true);
        } finally {
            context.draw();
            matrices.pop();
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.disableBlend();
        }
    }

    private static void frame(DrawContext context, LoadedStyle loaded, int width, int height) {
        Style.Frame f = loaded.style().frame();
        var horizontal = Slices.axis(f.region().width(), f.left(), f.right(), width);
        var vertical = Slices.axis(f.region().height(), f.top(), f.bottom(), height);
        for (int column = 0; column < 3; column++)
            for (int row = 0; row < 3; row++) {
                if (column == 1 && row == 1) continue; // The dedicated background region owns the center.
                Slices.Segment x = horizontal.get(column), y = vertical.get(row);
                sprite(context, loaded, new Style.Region(f.region().u() + x.source(), f.region().v() + y.source(),
                        x.sourceSize(), y.sourceSize()), x.target(), y.target(), x.targetSize(), y.targetSize());
            }
    }

    private static void sprite(DrawContext context, LoadedStyle loaded, Style.Region region, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || region.width() <= 0 || region.height() <= 0) return;
        RenderApi.texture(context, loaded.texture(), x, y, width, height, (float) region.u(), (float) region.v(),
                region.width(), region.height(), loaded.style().textureWidth(), loaded.style().textureHeight());
    }
}
