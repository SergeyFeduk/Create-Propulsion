package com.deltasf.createpropulsion.utility.tooltips;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public interface ITooltipProvider {
    public void addTooltip(ItemTooltipEvent event, List<Component> tooltipList);
}
