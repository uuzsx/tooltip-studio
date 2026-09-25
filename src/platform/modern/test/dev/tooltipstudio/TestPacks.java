package dev.tooltipstudio;
import net.minecraft.resource.*;
import java.nio.file.Path;
final class TestPacks {
    static ResourcePack directory(String name, Path path, boolean builtin) { return new DirectoryResourcePack(info(name), path); }
    static ResourcePack zip(String name, Path path) { return new ZipResourcePack.ZipBackedFactory(path).open(info(name)); }
    static ResourcePackInfo info(String name) { return new ResourcePackInfo(name, net.minecraft.text.Text.literal(name), ResourcePackSource.NONE, java.util.Optional.empty()); }
}
