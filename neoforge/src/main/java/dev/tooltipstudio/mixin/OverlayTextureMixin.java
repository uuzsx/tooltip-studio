package dev.tooltipstudio.mixin;

import dev.tooltipstudio.config.TextureFiles;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.InputStream;
import java.util.List;

@Mixin(CompositePackResources.class)
abstract class OverlayTextureMixin implements TextureFiles.RawTexturePack {
    @Shadow @Final private List<PackResources> packResourcesStack;

    @Override @Unique
    public IoSupplier<InputStream> tooltipstudio$openTexture(String namespace, String path) {
        for (PackResources layer : packResourcesStack) {
            var input = TextureFiles.openPack(layer, namespace, path);
            if (input != null) return input;
        }
        return null;
    }
}
