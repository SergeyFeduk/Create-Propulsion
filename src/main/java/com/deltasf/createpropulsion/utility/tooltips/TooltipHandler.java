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
    private static final List<ITooltipProvider> tooltipProviders = new ArrayList<>();
    static {
        tooltipProviders.add(new GenericSummaryTooltipProvider());
        tooltipProviders.add(new FuelTooltipProvider());
        tooltipProviders.add(new BladeTooltipProvider());
    }

    @SubscribeEvent
    public static void addToItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().isEmpty()) return;
        List<Component> tooltipList = new ArrayList<>();

        for(ITooltipProvider tooltipProvider : tooltipProviders) {
            tooltipProvider.addTooltip(event, tooltipList);
        }

        if (!tooltipList.isEmpty()) {
            event.getToolTip().addAll(1, tooltipList);
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
