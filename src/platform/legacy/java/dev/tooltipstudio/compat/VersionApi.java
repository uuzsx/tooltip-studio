package dev.tooltipstudio.compat;

import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.resource.Resource;
import net.minecraft.client.item.TooltipData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/** The small item-data API boundary between NBT and data-component versions. */
public final class VersionApi {
    private VersionApi() {}
    public static Identifier id(String value) { return new Identifier(value); }
    public static Identifier id(String namespace, String path) { return new Identifier(namespace, path); }
    public static String packName(Resource resource) { return resource.getResourcePackName(); }
    public static TooltipComponent tooltipComponent(Object data) { return TooltipComponent.of((TooltipData) data); }
    public static NbtCompound customData(ItemStack stack) { return stack.getNbt(); }
}
