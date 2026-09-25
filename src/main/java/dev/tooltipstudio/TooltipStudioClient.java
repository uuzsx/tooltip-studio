package dev.tooltipstudio;

import dev.tooltipstudio.compat.VersionApi;
import dev.tooltipstudio.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class TooltipStudioClient implements ClientModInitializer {
    public static final ConfigManager CONFIG = new ConfigManager();

    @Override
    public void onInitializeClient() {
        CONFIG.initialize();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId() { return VersionApi.id("tooltipstudio", "styles"); }
            @Override public void reload(ResourceManager manager) { CONFIG.reload(manager); }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("tooltipstudio")
                        .then(literal("reload").executes(context -> {
                            if (CONFIG.reload(MinecraftClient.getInstance().getResourceManager())) {
                                context.getSource().sendFeedback(Text.translatable("tooltipstudio.reload.success", CONFIG.count(), CONFIG.decorationCount()));
                                return 1;
                            }
                            context.getSource().sendError(Text.translatable("tooltipstudio.reload.error", CONFIG.lastError()));
                            return 0;
                        }))
                        .then(literal("list").executes(context -> {
                            context.getSource().sendFeedback(Text.literal(CONFIG.styleNames()));
                            return 1;
                        }))
                        .then(literal("decorations").executes(context -> {
                            context.getSource().sendFeedback(Text.literal(CONFIG.decorationNames()));
                            return 1;
                        }))));
    }
}
