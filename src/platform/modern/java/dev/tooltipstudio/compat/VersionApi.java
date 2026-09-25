package dev.tooltipstudio.compat;

import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.resource.Resource;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/** The small item-data API boundary between NBT and data-component versions. */
public final class VersionApi {
    private VersionApi() {}
    public static Identifier id(String value) { return Identifier.of(value); }
    public static Identifier id(String namespace, String path) { return Identifier.of(namespace, path); }
    public static String packName(Resource resource) { return resource.getPackId(); }
    public static TooltipComponent tooltipComponent(Object data) { return TooltipComponent.of((TooltipData) data); }
    public static NbtCompound customData(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? null : data.copyNbt();
    }
}
