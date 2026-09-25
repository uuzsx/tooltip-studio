package dev.tooltipstudio.smoke;




import com.google.gson.JsonParser;
import dev.tooltipstudio.TooltipStudioClient;
import dev.tooltipstudio.compat.ComponentMatcher;
import dev.tooltipstudio.compat.VersionApi;
import dev.tooltipstudio.config.NbtMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(value="tooltipstudio_smoke", dist=Dist.CLIENT)
public final class NeoSmoke {
    private boolean started;
    static int itemEvents, bundleEvents;
    private static ItemStack forest, artifact;
    private static List<String> previousPacks;
    public NeoSmoke(IEventBus bus) {
        SmokeComponents.register(bus);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderTooltipEvent.Pre e) -> {
            if (!e.getItemStack().isEmpty()) itemEvents++;
            if (e.getItemStack().is(Items.BUNDLE) && e.getComponents().size() > 1) bundleEvents++;
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var client = Minecraft.getInstance();
            if (!started && SmokeApi.ready(client)) {
                started = true;
                try {
                    check(TooltipStudioClient.CONFIG.count() == 1, "only default style bundled");
                    testComponents();
                    TextureProbe.run(client.gameDirectory.toPath());
                    createPack(client);
                    client.options.guiScale().set(2);
                    SmokeApi.resize(client);
                    var packs = client.getResourcePackRepository();
                    packs.reload();
                    previousPacks = List.copyOf(packs.getSelectedIds());
                    var enabled = new java.util.ArrayList<>(previousPacks);
                    enabled.add("file/tooltipstudio-component-smoke");
                    packs.setSelected(enabled);
                    client.reloadResourcePacks().thenRun(() -> {
                        try {
                            testSelection(client);
                            SmokeApi.show(client);
                        } catch (Exception e) { throw new IllegalStateException(e); }
                    }).exceptionally(error -> { error.printStackTrace(); client.stop(); return null; });
                } catch (Exception e) { throw new IllegalStateException("NEO SMOKE FAILED", e); }
            }
        });
    }
    static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        System.out.println("NEO SMOKE PASS: " + name);
    }
    private static boolean matches(ItemStack item, String json) {
        return ComponentMatcher.compile(JsonParser.parseString(json).getAsJsonObject().asMap()).test(item);
    }
    private static ItemStack sample(String location, Rarity rarity) throws Exception {
        ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(SmokeApi.nbt(
                "{Monumenta:{Location:'" + location + "',Tier:'artifact'},TooltipStyle:'missing'}")));
        item.set(DataComponents.RARITY, rarity);
        item.set(DataComponents.DAMAGE, 7);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Component Sword / " + location));
        item.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Custom data + component rules"),
                Component.literal("Centered title and fixed-end divider"), Component.literal("Animation, text segments and private PNG"))));
        return item;
    }
    private static void testComponents() throws Exception {
        var commandRegistries = SmokeApi.commands();
        var reader = new com.mojang.brigadier.StringReader(
                "minecraft:diamond_sword[minecraft:custom_data={Monumenta:{Location:\"forest\",Tier:\"artifact\"}},minecraft:rarity=\"epic\",minecraft:damage=7]");
        ItemStack commandItem = SmokeApi.item(commandRegistries, reader);
        check(!reader.canRead() && matches(commandItem, "{\"minecraft:custom_data\":{\"Monumenta.Location\":\"forest\"},\"minecraft:rarity\":\"epic\",\"minecraft:damage\":7}"),
                "documented give item syntax parses and matches components");
        try (var commandInput = NeoSmoke.class.getResourceAsStream("/modern-commands.json")) {
            var examples = JsonParser.parseString(new String(commandInput.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : examples.entrySet()) {
                var exampleReader = new com.mojang.brigadier.StringReader(entry.getValue().getAsString().substring("/give @s ".length()));
                var parsed = SmokeApi.item(commandRegistries, exampleReader);
                check(!exampleReader.canRead() && !parsed.isEmpty(), "ported example command: " + parsed.getItem());
            }
        }
        forest = sample("forest", Rarity.RARE);
        artifact = sample("forest", Rarity.EPIC);
        forest.set(SmokeComponents.QUALITY.get(), "legendary");
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
        check(ItemStack.isSameItemSameComponents(before, forest), "matching never mutates the item");
        try { ComponentMatcher.compile(JsonParser.parseString("{\"tooltipstudio:missing\":1}").getAsJsonObject().asMap());
            throw new AssertionError("unknown component accepted");
        } catch (IllegalArgumentException expected) { check(true, "unknown component rejected during compile"); }
    }
    private static void write(Path base, String name, String value) throws Exception {
        Path path = base.resolve(name); Files.createDirectories(path.getParent()); Files.writeString(path, value, StandardCharsets.UTF_8);
    }
    private static void createPack(Minecraft client) throws Exception {
        Path pack = client.gameDirectory.toPath().resolve("resourcepacks/tooltipstudio-component-smoke");
        write(pack, "pack.mcmeta", System.getProperty("tooltipstudio.packMetadata"));
        Path assets = pack.resolve("assets/tooltipstudio");
        var definition = JsonParser.parseString(new String(TooltipStudioClient.class.getResourceAsStream(
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
        try (var stream = TooltipStudioClient.class.getResourceAsStream("/assets/tooltipstudio/textures/styles/default.png")) {
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
    private static void testSelection(Minecraft client) throws Exception {
        var config = TooltipStudioClient.CONFIG;
        check(config.count() == 3, "resource-pack styles, mixed-case PNG and animated private PNG loaded");
        check(config.select(forest).style().minWidth() == 225, "old Monumenta NBT rule selects new component item");
        check(config.select(artifact).style().minWidth() == 255 && config.select(artifact).decorations().size() == 1,
                "component priority and independent component decoration rule");
        var override = artifact.copy();
        var nbt = VersionApi.customData(override); nbt.putString("TooltipStyle", "modern/forest");
        override.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        check(config.select(override).style().minWidth() == 225 && config.select(override).decorations().size() == 1,
                "TooltipStyle override from custom_data retains independent decoration");
        Path rules = client.gameDirectory.toPath().resolve("resourcepacks/tooltipstudio-component-smoke/assets/tooltipstudio/rules/modern/match.json");
        String original = Files.readString(rules);
        try {
            Files.writeString(rules, original.replace("minecraft:rarity", "tooltipstudio:missing"));
            check(!config.reload(client.getResourceManager()) && config.select(artifact).style().minWidth() == 255,
                    "bad component reload preserves all previous styles and textures");
        } finally { Files.writeString(rules, original); }
        check(config.reload(client.getResourceManager()), "component reload recovers");
    }

    static final AtomicInteger saved = new AtomicInteger();
    static int ticks;
    static ItemStack displayed() { return ticks < 40 ? forest : ticks < 80 ? artifact : SmokeApi.bundle(); }
    static int cursorX(int width) { return ticks < 40 ? 110 : width - 110; }
    static void tick(Minecraft client) {
        if (SmokeApi.overlayVisible(client)) return;
        ticks++;
        if (ticks == 20 || ticks == 23 || ticks == 60 || ticks == 100) {
            SmokeApi.capture(client, "neoforge-" + ticks + ".png", text -> saved.incrementAndGet());
        }
        if (ticks == 120) {
            check(itemEvents > 20, "native NeoForge tooltip events preserve the item stack");
            check(bundleEvents > 10, "vanilla bundle image component survives styled rendering");
        }
        if (ticks >= 130 && saved.get() == 4) {
            ticks = -100000;
            client.getResourcePackRepository().setSelected(previousPacks);
            client.reloadResourcePacks().thenRun(() -> {
                check(TooltipStudioClient.CONFIG.count() == 1 && TooltipStudioClient.CONFIG.decorationCount() == 0,
                        "disabling resource pack removes rules and decorations");
                System.out.println("NEO SMOKE COMPLETE"); client.stop();
            });
        }
        if (ticks > 400) throw new AssertionError("screenshot timeout");
    }
}
