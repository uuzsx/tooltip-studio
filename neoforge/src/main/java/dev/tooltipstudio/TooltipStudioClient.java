package dev.tooltipstudio;

import com.mojang.datafixers.util.Either;
import dev.tooltipstudio.compat.ClientPlatform;
import dev.tooltipstudio.compat.RenderApi;
import dev.tooltipstudio.config.ConfigManager;
import dev.tooltipstudio.render.StyledTooltipRenderer;
import dev.tooltipstudio.render.TitleData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import static net.minecraft.commands.Commands.literal;

@Mod(value = "tooltipstudio", dist = Dist.CLIENT)
public final class TooltipStudioClient {
    public static final ConfigManager CONFIG = new ConfigManager();
    public TooltipStudioClient(IEventBus modBus) {
        CONFIG.initialize();
        ClientPlatform.registerReload(modBus);
        modBus.addListener((RegisterClientTooltipComponentFactoriesEvent event) -> event.register(TitleData.class, RenderApi::title));
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::gather);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::render);
    }
    private void gather(RenderTooltipEvent.GatherComponents event) {
        var loaded = CONFIG.select(event.getItemStack());
        if (loaded == null || event.getTooltipElements().isEmpty()) return;
        var style = loaded.style();
        int cap = style.separator() != null && style.separator().enabled()
                ? style.separator().leftCap() + style.separator().rightCap() + style.separator().inset() * 2 + 1 : 1;
        int width = Math.max(cap, Math.min(style.maxWidth(), event.getScreenWidth() - style.padding().left() - style.padding().right() - 24));
        if (event.getMaxWidth() > 0) width = Math.min(width, event.getMaxWidth());
        event.setMaxWidth(width);
        var title = event.getTooltipElements().getFirst().left();
        if (title.isPresent()) event.getTooltipElements().set(0, Either.right(new TitleData(title.get(), width)));
    }
    private void render(RenderTooltipEvent.Pre event) {
        var loaded = CONFIG.select(event.getItemStack());
        if (loaded == null || event.getComponents().isEmpty()) return;
        StyledTooltipRenderer.draw(RenderApi.canvas(event.getGraphics(), event.getFont()), event.getComponents(),
                event.getX(), event.getY(), event.getTooltipPositioner(), loaded);
        event.setCanceled(true);
    }
    private void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(literal("tooltipstudio")
                .then(literal("reload").executes(context -> {
                    if (CONFIG.reload(Minecraft.getInstance().getResourceManager())) {
                        context.getSource().sendSuccess(() -> Component.translatable("tooltipstudio.reload.success", CONFIG.count(), CONFIG.decorationCount()), false);
                        return 1;
                    }
                    context.getSource().sendFailure(Component.translatable("tooltipstudio.reload.error", CONFIG.lastError()));
                    return 0;
                }))
                .then(literal("list").executes(context -> { context.getSource().sendSuccess(() -> Component.literal(CONFIG.styleNames()), false); return 1; }))
                .then(literal("decorations").executes(context -> { context.getSource().sendSuccess(() -> Component.literal(CONFIG.decorationNames()), false); return 1; })));
    }
}
