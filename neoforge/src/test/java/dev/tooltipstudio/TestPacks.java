package dev.tooltipstudio;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import java.nio.file.Path;
import java.util.Optional;
final class TestPacks {
    private static PackLocationInfo info(String name) { return new PackLocationInfo(name, Component.literal(name), PackSource.DEFAULT, Optional.empty()); }
    static PathPackResources directory(String name, Path path, boolean builtin) { return new PathPackResources(info(name), path); }
    static PackResources zip(String name, Path path) { return (PackResources) new FilePackResources.FileResourcesSupplier(path).openPrimary(info(name)); }
}
