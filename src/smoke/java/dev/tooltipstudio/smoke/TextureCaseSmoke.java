package dev.tooltipstudio.smoke;

import com.google.gson.JsonParser;
import dev.tooltipstudio.TooltipStudioClient;
import dev.tooltipstudio.config.TextureFiles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.OverlayResourcePack;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Reproduces reverie_fireR.png in folder/ZIP packs, inline animations, overlays, and local textures. */
final class TextureCaseSmoke extends Screen {
    private static final String REF = "tooltipstudio:textures/styles/valley/reverie_fireR.png";
    private static final String PNG = "assets/tooltipstudio/textures/styles/valley/reverie_fireR.png";
    private static final String[] PACKS = {"file/tooltip-studio-case-folder", "file/tooltip-studio-case.zip", "file/tooltip-studio-case-overlay"};
    private static final int RED = 0xff3456ef, BLUE = 0xffed9834;
    private final MinecraftClient minecraft;
    private final Runnable after;
    private final Path run, config;
    private final byte[] originalConfig;
    private final List<String> originalPacks;
    private final List<Path> temporary = new ArrayList<>();
    private final ItemStack sample = new ItemStack(Items.STICK);
    private SmokeClient.SlotHarness slots;
    private int phase, frames;
    private boolean waiting;

    private TextureCaseSmoke(MinecraftClient client, Runnable after) throws Exception {
        super(Text.literal("Mixed-case texture smoke")); minecraft = client; this.after = after;
        run = client.runDirectory.toPath().toAbsolutePath().normalize(); config = run.resolve("config/tooltipstudio");
        originalConfig = Files.readAllBytes(config.resolve("config.json"));
        originalPacks = List.copyOf(client.getResourcePackManager().getEnabledNames());
        sample.getOrCreateNbt().putString("TooltipStyle", "case_fixture/main");
    }
    static void start(MinecraftClient client, Runnable after) {
        TextureCaseSmoke screen = null;
        try { screen = new TextureCaseSmoke(client, after); screen.prepare(); screen.load(0); }
        catch (Exception | AssertionError e) {
            if (screen != null) screen.fail(e); else { e.printStackTrace(); client.scheduleStop(); }
            throw new IllegalStateException("TEXTURE CASE SMOKE FAILED", e);
        }
    }
    private void prepare() throws Exception {
        byte[] base;
        try (var in = getClass().getResourceAsStream("/assets/tooltipstudio/textures/styles/default.png")) { base = in.readAllBytes(); }
        Path localPng = config.resolve("textures/CaseFixture/Default.PNG"), localJson = config.resolve("styles/case_fixture/local.json");
        for (var p : List.of(localPng, localJson)) {
            check(!Files.exists(p), "case smoke does not overwrite existing local files");
            Files.createDirectories(p.getParent()); temporary.add(p);
        }
        Files.write(localPng, base); var local = SmokeClient.baseDefinition(); local.addProperty("texture", "LOCAL:CaseFixture/Default.PNG");
        Files.writeString(localJson, local.toString()); reload();
        sample.getOrCreateNbt().putString("TooltipStyle", "case_fixture/local");
        check(TooltipStudioClient.CONFIG.select(sample).style().texture().equals("LOCAL:CaseFixture/Default.PNG"),
                "local uppercase PNG and LOCAL prefix load without renaming files");
        sample.getOrCreateNbt().putString("TooltipStyle", "case_fixture/main");

        for (String name : List.of("tooltip-studio-case-folder", "tooltip-studio-case-overlay")) {
            Path pack = run.resolve("resourcepacks/" + name);
            write(pack.resolve("pack.mcmeta"), name.endsWith("overlay") ? """
                {"pack":{"pack_format":22,"description":"Mixed-case overlay test"},
                 "overlays":{"entries":[{"formats":22,"directory":"case_overlay"}]}}
                """ : "{\"pack\":{\"pack_format\":22,\"description\":\"Mixed-case folder test\"}}");
            Path atlas = pack.resolve("assets/tooltipstudio/textures/CaseFixture/BaseAtlas.PNG");
            Files.createDirectories(atlas.getParent()); Files.write(atlas, base);
            var style = SmokeClient.baseDefinition(); style.addProperty("texture", "TooltipStudio:textures/CaseFixture/BaseAtlas.PNG");
            var decoration = JsonParser.parseString("""
                {"texture":"tooltipstudio:textures/styles/valley/reverie_fireR.png","textureWidth":16,"textureHeight":64,
                 "region":{"u":0,"v":0,"width":16,"height":16},"animation":{"frames":4,"frameTime":4,"direction":"vertical"},
                 "anchor":"TOP_LEFT","x":-6,"y":-6,"foreground":true}
                """).getAsJsonObject();
            style.getAsJsonArray("decorations").add(decoration);
            write(pack.resolve("assets/tooltipstudio/styles/case_fixture/main.json"), style.toString());
            decoration = decoration.deepCopy(); decoration.addProperty("anchor", "TOP_RIGHT"); decoration.addProperty("x", 6);
            write(pack.resolve("assets/tooltipstudio/decorations/case_fixture/fire.json"), decoration.toString());
            write(pack.resolve("assets/tooltipstudio/rules/case_fixture/main.json"), """
                {"schemaVersion":1,"decorationRules":[{"decorations":["case_fixture/fire"],"items":["minecraft:stick"]}]}
                """);
            png(pack.resolve(PNG), RED);
            if (name.endsWith("overlay")) png(pack.resolve("case_overlay/" + PNG), BLUE);
        }
        Path folder = run.resolve("resourcepacks/tooltip-studio-case-folder");
        try (var zip = new ZipOutputStream(Files.newOutputStream(run.resolve("resourcepacks/tooltip-studio-case.zip")));
             var paths = Files.walk(folder)) {
            for (var file : paths.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(folder.relativize(file).toString().replace('\\', '/'))); Files.copy(file, zip); zip.closeEntry();
            }
        }
    }
    private static void write(Path file, String contents) throws Exception {
        Files.createDirectories(file.getParent()); Files.writeString(file, contents);
    }
    private static void png(Path file, int color) throws Exception {
        Files.createDirectories(file.getParent());
        try (var image = new NativeImage(16, 64, true)) {
            for (int y = 0; y < 64; y++) for (int x = 0; x < 16; x++) image.setColor(x, y, color);
            image.writeTo(file);
        }
    }
    private void load(int index) {
        waiting = true;
        reloadPacks(PACKS[index]).thenRun(() -> {
            try {
                check(TooltipStudioClient.CONFIG.lastError() == null, "mixed-case pack reload succeeds: " + PACKS[index]);
                var selected = TooltipStudioClient.CONFIG.select(sample);
                check(selected.inlineTextures().size() == 1 && selected.decorations().size() == 1,
                        "mixed-case base, inline animation and independent texture all load");
                try (var stream = TextureFiles.open(minecraft.getResourceManager(), config.resolve("textures"), REF);
                     var image = NativeImage.read(stream)) {
                    check(image.getWidth() == 16 && image.getHeight() == 64 && image.getColor(0, 0) == (index == 2 ? BLUE : RED),
                            "exact reverie_fireR.png is read with correct overlay priority");
                }
                if (index == 2) try (var packs = minecraft.getResourceManager().streamResourcePacks()) {
                    check(packs.anyMatch(p -> p instanceof OverlayResourcePack && p.getName().equals(PACKS[index])),
                            "vanilla selected the mixed-case overlay pack");
                }
                if (index == 0) {
                    Path file = run.resolve("resourcepacks/tooltip-studio-case-folder/assets/tooltipstudio/styles/case_fixture/main.json");
                    String good = Files.readString(file); var bad = JsonParser.parseString(good).getAsJsonObject();
                    bad.getAsJsonArray("decorations").get(0).getAsJsonObject().addProperty("texture", "tooltipstudio:textures/MissingR.PNG");
                    try {
                        Files.writeString(file, bad.toString());
                        check(!TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager())
                                        && TooltipStudioClient.CONFIG.select(sample).style() == selected.style(),
                                "missing mixed-case PNG preserves the complete previous snapshot");
                    } finally { Files.writeString(file, good); }
                    reload();
                }
                phase = index; frames = 0; waiting = false; minecraft.setScreen(this);
            } catch (Exception | AssertionError e) { fail(e); }
        }).exceptionally(e -> { fail(e); return null; });
    }
    private void reload() { check(TooltipStudioClient.CONFIG.reload(minecraft.getResourceManager()), "case fixture configuration reload"); }
    private CompletableFuture<Void> reloadPacks(String... extra) {
        var manager = minecraft.getResourcePackManager(); manager.scanPacks(); var names = new ArrayList<>(originalPacks);
        names.addAll(List.of(extra)); manager.setEnabledProfiles(names); return minecraft.reloadResources();
    }
    @Override protected void init() { slots = new SmokeClient.SlotHarness(); slots.init(minecraft, width, height); }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (waiting) return;
        try {
            context.fill(0, 0, width, height, 0xff253139); slots.show(context, sample, 140, 160); context.draw();
            if (++frames == 15) {
                try (var image = ScreenshotRecorder.takeScreenshot(minecraft.getFramebuffer())) {
                    int count = 0, color = phase == 2 ? BLUE : RED;
                    for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                        if (image.getColor(x, y) == color) count++;
                    check(count == 2 * 32 * 32, "both mixed-case animation decorations render at correct size");
                }
                if (phase < 2) load(phase + 1);
                else {
                    waiting = true; cleanup(); reloadPacks().thenRun(() -> {
                        check(TooltipStudioClient.CONFIG.count() == 1 && TooltipStudioClient.CONFIG.decorationCount() == 0,
                                "disabling mixed-case packs restores the original definitions");
                        try { check(Arrays.equals(originalConfig, Files.readAllBytes(config.resolve("config.json"))),
                                "mixed-case loading leaves local config unchanged"); }
                        catch (Exception e) { throw new IllegalStateException(e); }
                        after.run();
                    }).exceptionally(e -> { fail(e); return null; });
                }
            }
        } catch (Exception | AssertionError e) { fail(e); }
    }
    private void cleanup() throws Exception { for (Path file : temporary) Files.deleteIfExists(file); }
    private void fail(Throwable e) {
        waiting = true; System.err.println("TEXTURE CASE SMOKE FAILED"); e.printStackTrace();
        try { cleanup(); } catch (Exception failure) { failure.printStackTrace(); }
        reloadPacks().whenComplete((ignored, failure) -> minecraft.scheduleStop());
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); System.out.println("SMOKE PASS: " + message); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
