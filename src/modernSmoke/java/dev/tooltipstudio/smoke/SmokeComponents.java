package dev.tooltipstudio.smoke;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
/** A real third-party component registered only by the developer test mod. */
public final class SmokeComponents implements ModInitializer {
    static ComponentType<String> QUALITY;
    @Override public void onInitialize() {
        QUALITY = Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of("tooltipstudio_smoke", "quality"),
                ComponentType.<String>builder().codec(Codec.STRING).build());
    }
}
