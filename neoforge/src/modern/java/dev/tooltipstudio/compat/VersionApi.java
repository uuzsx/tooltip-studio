package dev.tooltipstudio.compat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;

public final class VersionApi {
    private VersionApi() {}
    public static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    public static ResourceLocation id(String namespace, String path) { return ResourceLocation.fromNamespaceAndPath(namespace, path); }
    public static String packName(Resource resource) { return resource.sourcePackId(); }
    public static CompoundTag customData(ItemStack stack) {
        var value = stack.get(DataComponents.CUSTOM_DATA);
        return value == null ? null : value.copyTag();
    }
    public static DataComponentType<?> component(ResourceLocation id) { return BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id); }
    public static String string(StringTag value) { return value.value(); }
    public static Number number(NumericTag value) { return value.box(); }
    public static byte byteValue(ByteTag value) { return value.byteValue(); }
    public static String stringKey(CompoundTag value, String key) { return value.getStringOr(key, ""); }
    public static DynamicTexture texture(NativeImage image) { return new DynamicTexture(() -> "Tooltip Studio runtime atlas", image); }
    public static void nearest(DynamicTexture texture) { /* DynamicTexture uses the nearest sampler by default. */ }
}
