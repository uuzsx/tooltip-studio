package dev.tooltipstudio;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
final class TestNbt {
    static CompoundTag parse(String text) throws CommandSyntaxException { return TagParser.parseTag(text); }
}
