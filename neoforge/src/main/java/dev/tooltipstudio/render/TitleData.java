package dev.tooltipstudio.render;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Keep all wrapped title lines together so they remain centered above the separator. */
public record TitleData(FormattedText text, int wrapWidth) implements TooltipComponent {}
