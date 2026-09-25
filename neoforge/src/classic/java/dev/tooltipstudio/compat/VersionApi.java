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
    public static DataComponentType<?> component(ResourceLocation id) { return BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(id).orElse(null); }
    public static String string(StringTag value) { return value.getAsString(); }
    public static Number number(NumericTag value) { return value.getAsNumber(); }
    public static byte byteValue(ByteTag value) { return value.getAsByte(); }
    public static String stringKey(CompoundTag value, String key) { return value.getString(key); }
    public static DynamicTexture texture(NativeImage image) { return new DynamicTexture(image); }
    public static void nearest(DynamicTexture texture) { texture.setFilter(false, false); }
}
