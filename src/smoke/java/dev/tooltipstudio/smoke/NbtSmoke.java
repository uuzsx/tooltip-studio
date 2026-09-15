package dev.tooltipstudio.smoke;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.tooltipstudio.TooltipStudioClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real client selection, hot reload and rendering checks; does not join a server or create a world. */
final class NbtSmoke extends Screen {
    // Temporary selection markers all use the one default PNG; no former preset graphics are needed.
    private static final List<String> VARIANTS = List.of("red_book", "epic", "mythical", "legendary", "uncommon", "cat_bell");
    private static final String RULES = """
            [
              {"style":"red_book","priority":500,"items":["minecraft:chainmail_chestplate"],"nbt":{"plain.display.Name":"Double Down"}},
              {"style":"epic","priority":400,"nbt":{"Monumenta.Location":"forest","Monumenta.Tier":"artifact"}},
              {"style":"mythical","priority":300,"nbt":{"Monumenta.Location":"forest"}},
              {"style":"legendary","priority":300,"nbt":{"Monumenta.Location":"forest"}},
              {"style":"uncommon","priority":275,"tags":["tooltipstudio:smoke_sticks"],"nbt":{"Monumenta.Region":"tag_test"}},
              {"style":"cat_bell","priority":250,"items":["minecraft:stick"],"nbt":{"Monumenta.Location":["city","valley"]}},
              {"style":"red_book","priority":240,"rarities":["uncommon"],"nbt":{"Monumenta.Location":"valley"}}
            ]
            """;
    private final Screen next;
    private final Path config;
    private final byte[] backup;
    private final java.util.ArrayList<Path> temporaryStyles = new java.util.ArrayList<>();
    private final AtomicBoolean saved = new AtomicBoolean();
    private boolean restored;
    private int frames;
    private SmokeClient.SlotHarness slots;
    private final ItemStack[] samples = {
            item(Items.STICK, "forest", null), item(Items.STICK, "city", null), item(Items.STICK, "desert", null),
            item(Items.STICK, "forest", "artifact"), item(Items.STICK, "Forest", null), new ItemStack(Items.STICK)
    };
    private static final String[] CAPTIONS = {
            "forest -> mythical", "city -> cat_bell", "desert -> rare (fallback)",
            "forest + artifact -> epic", "Forest -> rare (case sensitive)", "No Monumenta -> rare (fallback)"
    };

    private NbtSmoke(MinecraftClient client, Screen next) throws Exception {
        super(Text.literal("NBT rule verification"));
        this.next = next;
        config = client.runDirectory.toPath().resolve("config/tooltipstudio/config.json");
        backup = Files.readAllBytes(config);
    }

    static void start(MinecraftClient client, Screen next) {
        NbtSmoke screen = null;
        try {
            screen = new NbtSmoke(client, next);
            screen.verify(client);
            client.setScreen(screen);
        } catch (Exception | AssertionError failure) {
            if (screen != null) screen.restore(client);
            throw new IllegalStateException("NBT SMOKE FAILED", failure);
        }
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        System.out.println("SMOKE PASS: " + message);
    }

    private static ItemStack item(net.minecraft.item.Item item, String location, String tier) {
        ItemStack stack = new ItemStack(item);
        var nbt = stack.getOrCreateSubNbt("Monumenta");
        nbt.putString("Location", location);
        if (tier != null) nbt.putString("Tier", tier);
        return stack;
    }

    private static boolean style(ItemStack stack, String expected) {
        var selected = TooltipStudioClient.CONFIG.select(stack).style();
        return selected.texture().equals("tooltipstudio:textures/styles/default.png")
                && selected.offsetX() == VARIANTS.indexOf(expected) + 1;
    }

    private void verify(MinecraftClient client) throws Exception {
        for (String name : VARIANTS) {
            var variant = SmokeClient.baseDefinition();
            variant.addProperty("offsetX", VARIANTS.indexOf(name) + 1);
            Path file = config.getParent().resolve("styles/smoke_nbt/" + name + ".json");
            Files.createDirectories(file.getParent());
            Files.writeString(file, variant.toString());
            temporaryStyles.add(file);
        }
        var json = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
        json.addProperty("defaultStyle", "default");
        JsonArray rules = JsonParser.parseString(RULES).getAsJsonArray();
        for (var rule : rules) {
            var object = rule.getAsJsonObject();
            object.addProperty("style", "smoke_nbt/" + object.get("style").getAsString());
        }
        rules.add(JsonParser.parseString("""
                {"style":"smoke_nbt/legendary","priority":100,"items":["minecraft:netherite_*"]}
                """));
        rules.add(JsonParser.parseString("""
                {"style":"smoke_nbt/uncommon","priority":30,"rarities":["uncommon"]}
                """));
        rules.addAll(json.getAsJsonArray("rules"));
        json.add("rules", rules);
        Files.writeString(config, json.toString());
        check(TooltipStudioClient.CONFIG.reload(client.getResourceManager()), "NBT rules hot reload");

        check(style(samples[0], "mythical") && style(samples[1], "cat_bell") && style(samples[2], "rare"),
                "same vanilla item, different nested NBT -> different styles");
        check(style(samples[3], "epic") && style(samples[4], "rare") && style(samples[5], "rare"),
                "multiple paths AND, case sensitive strings, missing NBT fallback");
        var sword = item(Items.NETHERITE_SWORD, "forest", null);
        check(style(sword, "mythical"), "NBT priority beats lower ID rule; equal priority keeps JSON order");
        sword.removeSubNbt("Monumenta");
        check(style(sword, "legendary"), "existing ID rule still works");
        check(style(new ItemStack(Items.EXPERIENCE_BOTTLE), "uncommon"), "existing rarity rule still works");
        check(style(item(Items.EXPERIENCE_BOTTLE, "valley", null), "red_book")
                        && style(item(Items.DIAMOND, "valley", null), "rare"), "NBT AND rarity and NBT AND item ID");
        check(style(item(Items.STICK, "valley", null), "cat_bell"), "NBT alternative values OR");

        var entry = Items.STICK.getRegistryEntry();
        var previousTags = entry.streamTags().toList();
        // In this dev-only client no server has synchronized tags; temporarily bind one test tag.
        var bindTags = entry.getClass().getDeclaredMethod("setTags", java.util.Collection.class);
        bindTags.setAccessible(true);
        try {
            bindTags.invoke(entry, List.of(TagKey.of(RegistryKeys.ITEM, new Identifier("tooltipstudio", "smoke_sticks"))));
            var tagged = new ItemStack(Items.STICK);
            tagged.getOrCreateSubNbt("Monumenta").putString("Region", "tag_test");
            var other = new ItemStack(Items.DIAMOND);
            other.setNbt(tagged.getNbt().copy());
            check(style(tagged, "uncommon") && style(other, "rare") && style(new ItemStack(Items.STICK), "rare"),
                    "existing item tag condition AND nested NBT");
        } finally { bindTags.invoke(entry, previousTags); }

        var chestplate = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
        chestplate.setNbt(StringNbtReader.parse("{plain:{display:{Name:'Double Down'}},HideFlags:127}"));
        check(style(chestplate, "red_book"), "screenshot plain.display.Name example");
        var wrongBase = new ItemStack(Items.STICK);
        wrongBase.setNbt(chestplate.getNbt().copy());
        check(style(wrongBase, "rare"), "plain name rule requires the configured base item");
        chestplate.getOrCreateNbt().putString("TooltipStyle", "smoke_nbt/cat_bell");
        check(style(chestplate, "cat_bell"), "explicit TooltipStyle override still wins over NBT rules");
        var transported = ItemStack.fromNbt(samples[0].writeNbt(new NbtCompound()));
        var originalNbt = transported.getNbt().copy();
        check(style(transported, "mythical") && transported.getNbt().equals(originalNbt),
                "serialized item NBT is matched without modification");

        var bad = json.deepCopy();
        var badConditions = bad.getAsJsonArray("rules").get(0).getAsJsonObject().getAsJsonObject("nbt");
        badConditions.addProperty("Monumenta..Location", "forest");
        Files.writeString(config, bad.toString());
        check(!TooltipStudioClient.CONFIG.reload(client.getResourceManager()) && style(samples[0], "mythical"),
                "invalid NBT path rejects reload and retains previous selection");
        Files.writeString(config, json.toString());
        check(TooltipStudioClient.CONFIG.reload(client.getResourceManager()), "NBT configuration recovers");

        // Verify the exact command shipped with the example, without TooltipStyle overriding the rule.
        ItemStack command = new ItemStack(Items.STICK);
        command.setNbt(StringNbtReader.parse("{Monumenta:{Location:\"forest\"},display:{Lore:['{\"text\":\"NBT matching test\",\"italic\":false}']}}"));
        check(style(command, "mythical"), "documented forest test command matches");
        for (int i = 0; i < samples.length; i++) {
            samples[i].setCustomName(Text.literal("同一种木棍 / NBT 测试").styled(s -> s.withItalic(false)));
            var lore = new net.minecraft.nbt.NbtList();
            lore.add(net.minecraft.nbt.NbtString.of(Text.Serialization.toJsonString(Text.literal(CAPTIONS[i]))));
            samples[i].getOrCreateSubNbt("display").put("Lore", lore);
        }
    }

    @Override protected void init() {
        slots = new SmokeClient.SlotHarness();
        slots.init(client, width, height);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xff17202e);
        context.drawCenteredTextWithShadow(textRenderer, "Tooltip Studio 1.4 / NBT 匹配实测", width / 2, 14, 0xffffff);
        context.drawCenteredTextWithShadow(textRenderer, "全部为 minecraft:stick，未设置 TooltipStyle", width / 2, 31, 0x9ec5df);
        for (int i = 0; i < samples.length; i++) {
            int x = 18 + (i % 2) * (width / 2);
            int y = 76 + (i / 2) * 120;
            context.drawTextWithShadow(textRenderer, CAPTIONS[i], x, y - 17, 0xa8b8d0);
            context.drawItem(samples[i], x, y + 12);
            slots.show(context, samples[i], x + 18, y + 24);
        }
        context.draw();
        if (frames++ == 30) ScreenshotRecorder.saveScreenshot(client.runDirectory, "tooltip-studio-nbt-matching.png",
                client.getFramebuffer(), message -> saved.set(true));
        if (saved.get()) {
            restore(client);
            client.setScreen(next);
        }
        if (frames > 600) throw new IllegalStateException("NBT screenshot timed out");
    }

    private void restore(MinecraftClient client) {
        if (restored) return;
        try {
            Files.write(config, backup);
            for (Path file : temporaryStyles) Files.deleteIfExists(file);
            check(TooltipStudioClient.CONFIG.reload(client.getResourceManager()), "original config restored after NBT smoke");
            restored = true;
        } catch (Exception e) { throw new IllegalStateException("Could not restore smoke config", e); }
    }

    @Override public void removed() { restore(client); }
    @Override public boolean shouldPause() { return false; }
}
