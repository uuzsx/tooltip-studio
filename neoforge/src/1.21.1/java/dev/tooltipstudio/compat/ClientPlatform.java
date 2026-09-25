package dev.tooltipstudio.compat;
import dev.tooltipstudio.TooltipStudioClient;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
public final class ClientPlatform {
    private ClientPlatform() {}
    public static void registerReload(IEventBus bus) {
        bus.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener((ResourceManagerReloadListener) TooltipStudioClient.CONFIG::reload));
    }
}
