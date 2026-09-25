package dev.tooltipstudio;
import net.minecraft.resource.*;
import java.nio.file.Path;
final class TestPacks {
    static ResourcePack directory(String name, Path path, boolean builtin) { return new DirectoryResourcePack(name, path, builtin); }
    static ResourcePack zip(String name, Path path) { return new ZipResourcePack.ZipBackedFactory(path, false).open(name); }

}
