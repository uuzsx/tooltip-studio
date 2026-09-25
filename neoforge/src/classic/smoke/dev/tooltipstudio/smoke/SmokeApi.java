package dev.tooltipstudio.smoke;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
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
    static boolean overlayVisible(Minecraft mc) { return mc.getOverlay() != null; }
    static boolean ready(Minecraft mc) { return mc.isGameLoadFinished() && mc.getOverlay() == null; }
    static void resize(Minecraft mc) { mc.resizeDisplay(); }
    static CompoundTag nbt(String text) throws Exception { return TagParser.parseTag(text); }
    static CommandBuildContext commands() { return CommandBuildContext.simple(VanillaRegistries.createLookup(), FeatureFlags.DEFAULT_FLAGS); }
    static ItemStack item(CommandBuildContext context, StringReader text) throws Exception { return ItemArgument.item(context).parse(text).createItemStack(1, false); }
    static void show(Minecraft mc) { mc.setScreen(new Gallery()); }
    static void capture(Minecraft mc, String name, java.util.function.Consumer<Component> callback) { Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), callback); }
    static ItemStack bundle() {
        var stack = new ItemStack(Items.BUNDLE);
        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(java.util.List.of(new ItemStack(Items.DIAMOND, 8))));
        return stack;
    }
    static final class Gallery extends Screen {
        Gallery() { super(Component.literal("NeoForge tooltip smoke")); }
        @Override public void render(GuiGraphics g, int mx, int my, float delta) {
            g.fill(0, 0, width, height, 0xff18202e);
            g.drawCenteredString(font, "Tooltip Studio / NeoForge " + System.getProperty("tooltipstudio.mc"), width/2, 12, 0xffffffff);
            g.renderTooltip(font, NeoSmoke.displayed(), NeoSmoke.cursorX(width), 150);
        }
        @Override public void tick() { NeoSmoke.tick(Minecraft.getInstance()); }
        @Override public boolean isPauseScreen() { return false; }
    }
}
