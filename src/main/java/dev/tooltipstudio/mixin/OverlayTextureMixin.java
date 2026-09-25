package dev.tooltipstudio.mixin;

import dev.tooltipstudio.config.TextureFiles;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.OverlayResourcePack;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.InputStream;
import java.util.List;

@Mixin(OverlayResourcePack.class)
abstract class OverlayTextureMixin implements TextureFiles.RawTexturePack {
    @Shadow @Final private List<ResourcePack> overlaysAndBase;

    @Override @Unique
    public InputSupplier<InputStream> tooltipstudio$openTexture(String namespace, String path) {
        for (ResourcePack layer : overlaysAndBase) {
            var input = TextureFiles.openPack(layer, namespace, path);
            if (input != null) return input;
        }
        return null;
    }
}
