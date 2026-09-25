package dev.tooltipstudio.smoke;

import dev.tooltipstudio.config.TextureFiles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Exercise actual directory, ZIP and overlay pack implementations with mixins loaded. */
final class TextureProbe {
    static void run(Path root) throws Exception {
        var info = new PackLocationInfo("case-probe", Component.literal("case-probe"), PackSource.DEFAULT, Optional.empty());
        String path = "textures/Case/FireR.PNG", entry = "assets/tooltipstudio/" + path;
        Path base = root.resolve("case-probe/base"), overlay = root.resolve("case-probe/overlay");
        Files.createDirectories(base.resolve(entry).getParent());
        Files.createDirectories(overlay.resolve(entry).getParent());
        Files.writeString(base.resolve(entry), "BASE", StandardCharsets.UTF_8);
        Files.writeString(overlay.resolve(entry), "OVERLAY", StandardCharsets.UTF_8);
        try (var pack = new PathPackResources(info, base)) {
            NeoSmoke.check(pack instanceof TextureFiles.RawTexturePack, "directory mixin loaded from mod JAR");
            read(pack, path, "BASE");
        }
        Path zipPath = root.resolve("case-probe/archive.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zip.putNextEntry(new ZipEntry(entry)); zip.write("ZIP".getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
        }
        boolean newest = System.getProperty("tooltipstudio.mc").equals("26.3");
        var supplier = new FilePackResources.FileResourcesSupplier(zipPath);
        // The developer probe spans the 26.3 metadata API split; production uses version adapters.
        try (var pack = (PackResources) supplier.getClass().getMethod(newest ? "openMetadata" : "openPrimary", PackLocationInfo.class).invoke(supplier, info)) {
            read(pack, path, "ZIP");
        }
        var type = Class.forName("net.minecraft.server.packs." + (newest ? "OverlayedPackResources" : "CompositePackResources"));
        try (var pack = (PackResources) type.getConstructor(PackResources.class, List.class).newInstance(
                new PathPackResources(info, base), List.of(new PathPackResources(info, overlay)))) {
            NeoSmoke.check(pack instanceof TextureFiles.RawTexturePack, "overlay mixin loaded from mod JAR");
            read(pack, path, "OVERLAY");
        }
    }
    private static void read(PackResources pack, String path, String expected) throws Exception {
        try (var input = TextureFiles.openPack(pack, "tooltipstudio", path).get()) {
            NeoSmoke.check(new String(input.readAllBytes(), StandardCharsets.UTF_8).equals(expected), "mixed-case raw texture: " + expected);
        }
    }
}
