package dev.tooltipstudio.smoke;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
final class SmokeComponents {
    static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "tooltipstudio_smoke");
    static final java.util.function.Supplier<DataComponentType<String>> QUALITY = TYPES.register("quality", () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());
    static void register(IEventBus bus) { TYPES.register(bus); }
}
