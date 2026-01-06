package com.deltasf.createpropulsion.utility.tooltips;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class HeatTooltipProvider implements ITooltipProvider {
    private enum HeatAmount {LOW, MODERATE, HIGH}
    private static LazyOptional<Map<Integer, HeatInfo>> idsToTooltips = LazyOptional.empty();

    private static void registerHeatTooltip(BlockEntry<?> blockEntry, HeatInfo info) {
        Item item = blockEntry.get().asItem();
        idsToTooltips.resolve().get().put(Item.getId(item), info);
    }

    private static void populateTooltips() {
        registerHeatTooltip(PropulsionBlocks.SOLID_BURNER, new HeatInfo(true, HeatAmount.MODERATE));
        registerHeatTooltip(PropulsionBlocks.LIQUID_BURNER, new HeatInfo(true, HeatAmount.HIGH));
        registerHeatTooltip(PropulsionBlocks.STIRLING_ENGINE_BLOCK, new HeatInfo(false, HeatAmount.MODERATE));
    }

    @Override
    public void addTooltip(ItemTooltipEvent event, List<Component> tooltipList) {
        //Resolve map
        if (!idsToTooltips.isPresent()) {
            idsToTooltips = LazyOptional.of(() -> new HashMap<Integer, HeatInfo>());
            populateTooltips();
        }
        Map<Integer, HeatInfo> tooltipMap = idsToTooltips.resolve().get();

        ItemStack stack = event.getItemStack();
        if (!stack.is((h) -> tooltipMap.containsKey(Item.getId(h.get())))) return;

        HeatInfo info = tooltipMap.get(Item.getId(stack.getItem()));
        Component heatEffectLine = Component.translatable(info.getEffectLineKey()).append(Component.literal(":")).withStyle(ChatFormatting.GRAY);
        Component heatLine = Component.literal(TooltipHelper.makeProgressBar(3, info.getHeatAmountOrdinal() + 1))
            .append(Component.literal(" "))
            .append(CreateLang.translate(info.getHeatAmountKey()).component())
            .withStyle(info.getHeatAmountColor());

        tooltipList.add(Component.empty());
        tooltipList.add(heatEffectLine);
        tooltipList.add(heatLine);
    }

    private record HeatInfo(boolean isSource, HeatAmount heatAmount) {
        public ChatFormatting getHeatAmountColor() {
            return switch (heatAmount()) {
                case LOW -> ChatFormatting.GREEN;
                case MODERATE -> ChatFormatting.GOLD;
                case HIGH -> ChatFormatting.RED;
            };
        }

        public int getHeatAmountOrdinal() {
            return switch (heatAmount()) {
                case LOW -> 0;
                case MODERATE -> 1;
                case HIGH -> 2;
            };
        }

        public String getHeatAmountKey() {
            return switch (heatAmount()) {
                case LOW -> "tooltip.stressImpact.low";
                case MODERATE -> "tooltip.stressImpact.medium";
                case HIGH -> "tooltip.stressImpact.high";
            };
        }

        public String getEffectLineKey() {
            if (isSource) {
                return "createpropulsion.tooltip.heat.generation";
            } else {
                return "createpropulsion.tooltip.heat.consumption";
            }
        }
    }
}
