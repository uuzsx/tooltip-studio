package dev.tooltipstudio.smoke;

import com.google.gson.JsonParser;
import dev.tooltipstudio.TooltipStudioClient;
import dev.tooltipstudio.compat.ComponentMatcher;
import dev.tooltipstudio.compat.VersionApi;
import dev.tooltipstudio.config.NbtMatcher;
import dev.tooltipstudio.render.TooltipRenderScope;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class ModernSmokeClient implements ClientModInitializer {
    private boolean started;
    private static ItemStack forest, artifact;
    private static List<String> previousPacks;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!started && client.currentScreen instanceof TitleScreen && client.getOverlay() == null) {
                started = true;
                try {
                    check(TooltipStudioClient.CONFIG.count() == 1, "only default style bundled");
                    testComponents();
                    createPack(client);
                    client.options.getGuiScale().setValue(2);
                    client.onResolutionChanged();
                    var packs = client.getResourcePackManager();
                    packs.scanPacks();
                    previousPacks = List.copyOf(packs.getEnabledIds());
                    var enabled = new java.util.ArrayList<>(previousPacks);
                    enabled.add("file/tooltipstudio-component-smoke");
                    packs.setEnabledProfiles(enabled);
                    client.reloadResources().thenRun(() -> {
                        try {
                            testSelection(client);
                            client.setScreen(new Gallery());
                        } catch (Exception e) { throw new IllegalStateException(e); }
                    }).exceptionally(error -> { error.printStackTrace(); client.scheduleStop(); return null; });
                } catch (Exception e) { throw new IllegalStateException("MODERN SMOKE FAILED", e); }
            }
        });
    }
    static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        System.out.println("MODERN SMOKE PASS: " + name);
    }
    private static boolean matches(ItemStack item, String json) {
        return ComponentMatcher.compile(JsonParser.parseString(json).getAsJsonObject().asMap()).test(item);
    }
    private static ItemStack sample(String location, Rarity rarity) throws Exception {
        ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(StringNbtReader.parse(
                "{Monumenta:{Location:'" + location + "',Tier:'artifact'},TooltipStyle:'missing'}")));
        item.set(DataComponentTypes.RARITY, rarity);
        item.set(DataComponentTypes.DAMAGE, 7);
        item.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Component Sword / " + location));
        item.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("Custom data + component rules"),
                Text.literal("Centered title and fixed-end divider"), Text.literal("Animation, text segments and private PNG"))));
        return item;
    }
    private static void testComponents() throws Exception {
        var commandRegistries = net.minecraft.command.CommandRegistryAccess.of(
                net.minecraft.registry.BuiltinRegistries.createWrapperLookup(),
                net.minecraft.resource.featuretoggle.FeatureFlags.DEFAULT_ENABLED_FEATURES);
        var reader = new com.mojang.brigadier.StringReader(
                "minecraft:diamond_sword[minecraft:custom_data={Monumenta:{Location:\"forest\",Tier:\"artifact\"}},minecraft:rarity=\"epic\",minecraft:damage=7]");
        ItemStack commandItem = net.minecraft.command.argument.ItemStackArgumentType.itemStack(commandRegistries).parse(reader).createStack(1, false);
        check(!reader.canRead() && matches(commandItem, "{\"minecraft:custom_data\":{\"Monumenta.Location\":\"forest\"},\"minecraft:rarity\":\"epic\",\"minecraft:damage\":7}"),
                "documented give item syntax parses and matches components");
        try (var commandInput = ModernSmokeClient.class.getResourceAsStream("/modern-commands.json")) {
            var examples = JsonParser.parseString(new String(commandInput.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : examples.entrySet()) {
                var exampleReader = new com.mojang.brigadier.StringReader(entry.getValue().getAsString().substring("/give @s ".length()));
                var parsed = net.minecraft.command.argument.ItemStackArgumentType.itemStack(commandRegistries).parse(exampleReader).createStack(1, false);
                check(!exampleReader.canRead() && !parsed.isEmpty(), "ported example command: " + parsed.getItem());
            }
        }
        forest = sample("forest", Rarity.RARE);
        artifact = sample("forest", Rarity.EPIC);
        forest.set(SmokeComponents.QUALITY, "legendary");
        check(matches(forest, "{\"tooltipstudio_smoke:quality\":\"legendary\"}"), "registered modded component codec");
        var before = forest.copy();
        check(matches(forest, """
                {"minecraft:custom_data":{"Monumenta.Location":"forest"},"minecraft:rarity":["rare","epic"],"minecraft:damage":7}
                """), "component IDs AND, scalar alternatives OR, nested custom-data path");
        check(!matches(forest, "{\"minecraft:rarity\":\"epic\"}"), "component mismatch");
        check(!matches(forest, "{\"minecraft:custom_data\":{\"Monumenta.Missing\":\"forest\"}}"), "missing component path");
        check(!matches(new ItemStack(Items.STICK), "{\"minecraft:damage\":0}"), "absent component is not its expected value");
        check(matches(new ItemStack(Items.STICK), "{\"minecraft:rarity\":\"common\"}"), "effective default components are included");
        check(!matches(forest, "{\"minecraft:damage\":\"7\"}"), "component numeric/string types stay distinct");
        check(NbtMatcher.compile(JsonParser.parseString("{\"Monumenta.Location\":\"forest\"}").getAsJsonObject().asMap())
                .get(0).matches(VersionApi.customData(forest)), "legacy nbt path reads minecraft:custom_data");
        check(ItemStack.areItemsAndComponentsEqual(before, forest), "matching never mutates the item");
        try { ComponentMatcher.compile(JsonParser.parseString("{\"tooltipstudio:missing\":1}").getAsJsonObject().asMap());
            throw new AssertionError("unknown component accepted");
        } catch (IllegalArgumentException expected) { check(true, "unknown component rejected during compile"); }
    }
    private static void write(Path base, String name, String value) throws Exception {
        Path path = base.resolve(name); Files.createDirectories(path.getParent()); Files.writeString(path, value, StandardCharsets.UTF_8);
    }
    private static void createPack(MinecraftClient client) throws Exception {
        Path pack = client.runDirectory.toPath().resolve("resourcepacks/tooltipstudio-component-smoke");
        write(pack, "pack.mcmeta", "{\"pack\":{\"pack_format\":" + Integer.getInteger("tooltipstudio.packFormat", 34) + ",\"description\":\"Developer smoke\"}}");
        Path assets = pack.resolve("assets/tooltipstudio");
        var definition = JsonParser.parseString(new String(ModernSmokeClient.class.getResourceAsStream(
                "/assets/tooltipstudio/defaults/styles/default.json").readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        definition.addProperty("texture", "tooltipstudio:textures/modern/BaseAtlas.PNG");
        definition.addProperty("minWidth", 225);
        definition.getAsJsonObject("separator").addProperty("marginTop", 15);
        definition.addProperty("offsetX", 14);
        definition.addProperty("offsetY", -6);
        definition.add("decorations", JsonParser.parseString("""
            [{"texture":"tooltipstudio:textures/modern/reverie_fireR.png","textureWidth":16,"textureHeight":64,
              "region":{"u":0,"v":0,"width":16,"height":16},"animation":{"frames":4,"frameTime":2,"direction":"vertical"},
              "anchor":"TOP_LEFT","x":-4,"y":-4,"foreground":true,"x_scale":1.5,"y_scale":1.5}]
            """));
        write(assets, "styles/modern/forest.json", definition.toString());
        definition.addProperty("minWidth", 255);
        write(assets, "styles/modern/artifact.json", definition.toString());
        Files.createDirectories(assets.resolve("textures/modern"));
        try (var stream = ModernSmokeClient.class.getResourceAsStream("/assets/tooltipstudio/textures/styles/default.png")) {
            Files.copy(stream, assets.resolve("textures/modern/BaseAtlas.PNG"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        BufferedImage image = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        int[] colors = {0xffee5544, 0xff66dd44, 0xff44aaff, 0xffdd55ee};
        for (int y = 0; y < 64; y++) for (int x = 0; x < 16; x++) image.setRGB(x, y, colors[y / 16]);
        ImageIO.write(image, "PNG", assets.resolve("textures/modern/reverie_fireR.png").toFile());
        write(assets, "decorations/modern/badge.json", """
            {"schemaVersion":1,"type":"text","segments":[{"text":"Architect's Ring: ","color":"#888888"},{"text":"Artifact","color":"#FF5555"}],
             "anchor":"SEPARATOR_CENTER","x":0,"y":-8,"foreground":true,"x_scale":0.8,"y_scale":0.8}
            """);
        write(assets, "rules/modern/match.json", """
            {"schemaVersion":1,"rules":[
              {"style":"modern/forest","priority":200,"nbt":{"Monumenta.Location":"forest"}},
              {"style":"modern/artifact","priority":300,"items":["minecraft:diamond_sword"],
               "components":{"minecraft:rarity":"epic","minecraft:custom_data":{"Monumenta.Tier":"artifact"}}}],
             "decorationRules":[{"decorations":["modern/badge"],"components":{"minecraft:rarity":"epic"}}]}
            """);
    }
    private static void testSelection(MinecraftClient client) throws Exception {
        var config = TooltipStudioClient.CONFIG;
        check(config.count() == 3, "resource-pack styles, mixed-case PNG and animated private PNG loaded");
        check(config.select(forest).style().minWidth() == 225, "old Monumenta NBT rule selects new component item");
        check(config.select(artifact).style().minWidth() == 255 && config.select(artifact).decorations().size() == 1,
                "component priority and independent component decoration rule");
        var override = artifact.copy();
        var nbt = VersionApi.customData(override); nbt.putString("TooltipStyle", "modern/forest");
        override.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        check(config.select(override).style().minWidth() == 225 && config.select(override).decorations().size() == 1,
                "TooltipStyle override from custom_data retains independent decoration");
        Path rules = client.runDirectory.toPath().resolve("resourcepacks/tooltipstudio-component-smoke/assets/tooltipstudio/rules/modern/match.json");
        String original = Files.readString(rules);
        try {
            Files.writeString(rules, original.replace("minecraft:rarity", "tooltipstudio:missing"));
            check(!config.reload(client.getResourceManager()) && config.select(artifact).style().minWidth() == 255,
                    "bad component reload preserves all previous styles and textures");
        } finally { Files.writeString(rules, original); }
        check(config.reload(client.getResourceManager()), "component reload recovers");
    }
    private static final class Gallery extends Screen {
        private int frames;
        private final AtomicInteger saved = new AtomicInteger();
        private SlotHarness inventory;
        private ModernShulkerSmoke shulker;
        Gallery() { super(Text.literal("Tooltip Studio component smoke")); }
        @Override protected void init() { inventory = new SlotHarness(); inventory.init(client, width, height);
            if (TooltipRenderScope.SHULKER_LOADED) shulker = new ModernShulkerSmoke(client, width, height); }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(0,0,width,height,0xff18202e);
            context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio / "+net.minecraft.SharedConstants.getGameVersion().getName(), width/2, 10, 0xffffff);
            if (shulker != null && frames >= 45) shulker.render(context, textRenderer);
            else {
            inventory.show(context, forest, 10, 70);
            context.drawItemTooltip(textRenderer, artifact, width-10, 250);
            var bundle = new ItemStack(Items.BUNDLE);
            bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(List.of(new ItemStack(Items.DIAMOND,8))));
            inventory.show(context, bundle, 20, height-60);
            checkScope();
            }
            context.draw();
            if (frames == 20 || frames == 35 || (shulker != null && frames == 80)) {
                ScreenshotRecorder.saveScreenshot(client.runDirectory, "components-"+frames+".png", client.getFramebuffer(), text -> saved.incrementAndGet());
                System.out.println("MODERN SMOKE CAPTURE: components-"+frames+".png");
            }
            frames++;
            if (frames > (shulker == null ? 65 : 100) && saved.get() == (shulker == null ? 2 : 3)) {
                client.getResourcePackManager().setEnabledProfiles(previousPacks);
                client.reloadResources().thenRun(() -> {
                    check(TooltipStudioClient.CONFIG.count() == 1 && TooltipStudioClient.CONFIG.decorationCount() == 0,
                            "disabling resource pack removes component rules and decorations");
                    System.out.println("MODERN SMOKE COMPLETE"); client.scheduleStop();
                });
                frames = -100000; // Wait for the asynchronous reload once.
            }
            if (frames > 500) throw new IllegalStateException("screenshot timeout");
        }
        @Override public boolean shouldPause() { return false; }
    }
    private static void checkScope() {
        if (TooltipRenderScope.item() != null) throw new AssertionError("item tooltip scope leaked");
    }
    static final class SlotHarness extends HandledScreen<EmptyHandler> {
        private final SimpleInventory inventory = new SimpleInventory(2);
        SlotHarness() { super(new EmptyHandler(), new PlayerInventory(null), Text.literal("Test")); }
        void show(DrawContext context, ItemStack stack, int x, int y) {
            showSlot(context, 0, stack, x, y);
        }
        void showSlot(DrawContext context, int index, ItemStack stack, int x, int y) {
            inventory.setStack(index,stack); focusedSlot = new Slot(inventory,index,0,0);
            drawMouseoverTooltip(context,x,y); checkScope();
        }
        @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY) {}
    }
    private static final class EmptyHandler extends ScreenHandler {
        EmptyHandler() { super(null,0); }
        @Override public ItemStack quickMove(PlayerEntity player,int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return true; }
    }
}
