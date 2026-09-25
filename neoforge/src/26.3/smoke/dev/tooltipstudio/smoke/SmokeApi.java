package dev.tooltipstudio.smoke;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.flag.FeatureFlags;
import com.mojang.brigadier.StringReader;
final class SmokeApi {
    static boolean overlayVisible(Minecraft mc) { return mc.gui.overlay() != null; }
    private static boolean worldRequested;
    static boolean ready(Minecraft mc) {
        if (dev.tooltipstudio.TooltipStudioClient.CONFIG.count() == 0 || mc.gui.overlay() != null) return false;
        if (!worldRequested) {
            worldRequested = true;
            var settings = new net.minecraft.world.level.LevelSettings("Tooltip smoke", net.minecraft.world.level.GameType.CREATIVE,
                    new net.minecraft.world.level.LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL, false, false),
                    true, net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("tooltip-smoke-" + System.currentTimeMillis(), settings,
                    new net.minecraft.world.level.levelgen.WorldOptions(1, false, false),
                    lookup -> lookup.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),
                    new net.minecraft.client.gui.screens.TitleScreen());
        }
        return mc.level != null && mc.player != null && mc.gui.screen() == null;
    }
    static void resize(Minecraft mc) { mc.resizeGui(); }
    static CompoundTag nbt(String text) throws Exception { return TagParser.parseCompoundFully(text); }
    static CommandBuildContext commands() {
        // In 26.x item defaults are bound from world registries, so test in a real disposable world.
        return CommandBuildContext.simple(Minecraft.getInstance().level.registryAccess(), FeatureFlags.DEFAULT_FLAGS);
    }
    static ItemStack item(CommandBuildContext context, StringReader text) throws Exception { return ItemArgument.item(context).parse(text).createItemStack(1); }
    static void show(Minecraft mc) { mc.setScreenAndShow(new Gallery()); }
    static void capture(Minecraft mc, String name, java.util.function.Consumer<Component> callback) { Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, callback); }
    static ItemStack bundle() {
        var stack = new ItemStack(Items.BUNDLE);
        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(java.util.List.of(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.DIAMOND, 8)))));
        return stack;
    }
    static final class Gallery extends Screen {
        Gallery() { super(Component.literal("NeoForge tooltip smoke")); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(0, 0, width, height, 0xff18202e);
            g.centeredText(font, "Tooltip Studio / NeoForge " + System.getProperty("tooltipstudio.mc"), width/2, 12, 0xffffffff);
            g.setTooltipForNextFrame(font, NeoSmoke.displayed(), NeoSmoke.cursorX(width), 150);
        }
        @Override public void tick() { NeoSmoke.tick(Minecraft.getInstance()); }
        @Override public boolean isPauseScreen() { return false; }
    }
}
