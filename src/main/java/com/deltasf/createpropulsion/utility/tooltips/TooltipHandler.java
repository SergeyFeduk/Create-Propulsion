package com.deltasf.createpropulsion.utility.tooltips;

import com.deltasf.createpropulsion.CreatePropulsion;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public class TooltipHandler {
    private static final List<ITooltipProvider> topProviders = new ArrayList<>();
    private static final List<ITooltipProvider> bottomProviders = new ArrayList<>();

    static {
        topProviders.add(new GenericSummaryTooltipProvider());
        topProviders.add(new FuelTooltipProvider());
        topProviders.add(new BladeTooltipProvider());

        bottomProviders.add(new HeatTooltipProvider());
    }

    @SubscribeEvent()
    public static void addToItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().isEmpty()) return;
        List<Component> currentTooltip = event.getToolTip();

        //Handle top list
        List<Component> topList = new ArrayList<>();
        for(ITooltipProvider tooltipProvider : topProviders) {
            tooltipProvider.addTooltip(event, topList);
        }

        if (!topList.isEmpty()) {
            int index = currentTooltip.isEmpty() ? 0 : 1; 
            currentTooltip.addAll(index, topList);
        }

        //Handle bottom list
        List<Component> bottomList = new ArrayList<>();
        for(ITooltipProvider tooltipProvider : bottomProviders) {
            tooltipProvider.addTooltip(event, bottomList);
        }

        if (!bottomList.isEmpty()) {
            currentTooltip.addAll(bottomList);
        }
    }

    public static void wrapShiftHoldText(List<Component> tooltipList, String langKey, Runnable addDetailedContent) {
        boolean isShiftDown = Screen.hasShiftDown();
        Component keyComponent = Component.translatable("create.tooltip.keyShift")
            .withStyle(isShiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY);

        tooltipList.add(Component.translatable(langKey, keyComponent).withStyle(ChatFormatting.DARK_GRAY));

        if (isShiftDown) {
            tooltipList.add(Component.empty());
            addDetailedContent.run();
        }
    }
}
