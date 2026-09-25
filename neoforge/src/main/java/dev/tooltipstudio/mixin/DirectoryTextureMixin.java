package dev.tooltipstudio.mixin;

import dev.tooltipstudio.config.TextureFiles;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.InputStream;
import java.nio.file.Path;

@Mixin(PathPackResources.class)
abstract class DirectoryTextureMixin implements TextureFiles.RawTexturePack {
    @Shadow @Final private Path root;

    @Override @Unique
    public IoSupplier<InputStream> tooltipstudio$openTexture(String namespace, String path) {
        return TextureFiles.openDirectory(root, namespace, path);
    }
}
