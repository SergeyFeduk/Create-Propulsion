package com.deltasf.createpropulsion.utility;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.PropulsionBlocks;
import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.thruster.ThrusterBlockEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import com.simibubi.create.foundation.item.TooltipHelper.Palette;

import net.minecraft.network.chat.Component;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public class TooltipHandler {
    private static HashMap<Item, Function<SummaryPayload, String>> tooltipModificationLookup = new HashMap<Item, Function<SummaryPayload, String>>();

    @SubscribeEvent
    public static void addToItemTooltip(ItemTooltipEvent event) {
        //Looked this up in CDG
        Item item = event.getItemStack().getItem();
        //Skip all items not from this mod
        if(!ForgeRegistries.ITEMS.getKey(item).getNamespace().equals(CreatePropulsion.ID))
            return;
        String path = CreatePropulsion.ID + "." + ForgeRegistries.ITEMS.getKey(item).getPath();
        if (tooltipModificationLookup.isEmpty()) populateModifiables();

        List<Component> tooltip = event.getToolTip();
        //Add Create "Hold Shift for summary"
        List<Component> tooltipList = new ArrayList<>();
        if(I18n.exists(path + ".tooltip.summary")) {
            if (Screen.hasShiftDown()) {
                tooltipList.add(Lang.translateDirect("tooltip.holdForDescription", Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.DARK_GRAY));
                tooltipList.add(Component.empty());

                //Handle modifiable summaries
                var tooltipModifier = tooltipModificationLookup.get(item);
                if (tooltipModifier != null) {
                    String summary = tooltipModifier.apply(new SummaryPayload(item, path));
                    tooltipList.addAll(TooltipHelper.cutStringTextComponent(summary, Palette.STANDARD_CREATE));
                } else {
                    tooltipList.addAll(TooltipHelper.cutStringTextComponent(Component.translatable(path + ".tooltip.summary").getString(), Palette.STANDARD_CREATE));
                }

                //Yeah this only supports up to 2 conditions
                if(!Component.translatable(path + ".tooltip.condition1").getString().equals(path + ".tooltip.condition1")) {
                    tooltipList.add(Component.empty());
                    tooltipList.add(Component.translatable(path + ".tooltip.condition1").withStyle(ChatFormatting.GRAY));
                    tooltipList.addAll(TooltipHelper.cutStringTextComponent(Component.translatable(path + ".tooltip.behaviour1").getString(), Palette.STANDARD_CREATE.primary(), Palette.STANDARD_CREATE.highlight(), 1));
                    if(!Component.translatable(path + ".tooltip.condition2").getString().equals(path + ".tooltip.condition2")) {
                        tooltipList.add(Component.translatable(path + ".tooltip.condition2").withStyle(ChatFormatting.GRAY));
                        tooltipList.addAll(TooltipHelper.cutStringTextComponent(Component.translatable(path + ".tooltip.behaviour2").getString(), Palette.STANDARD_CREATE.primary(), Palette.STANDARD_CREATE.highlight(), 1));
                    }
                }

            } else {
                tooltipList.add(Lang.translateDirect("tooltip.holdForDescription", Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.addAll(1, tooltipList);
    }

    private static void populateModifiables() {
        //Thruster
        tooltipModificationLookup.put(PropulsionBlocks.THRUSTER_BLOCK.asItem(), (payload) -> {
            float thrustMultiplier = (float)(double)PropulsionConfig.THRUSTER_THRUST_MULTIPLIER.get();
            int thrusterStrength = Math.round(ThrusterBlockEntity.BASE_MAX_THRUST / 1000.0f * thrustMultiplier);
            return Component.translatable(payload.path + ".tooltip.summary").getString().replace("{}", String.valueOf(thrusterStrength));
        });
        //Inline optical sensor
        tooltipModificationLookup.put(PropulsionBlocks.INLINE_OPTICAL_SENSOR_BLOCK.asItem(), (payload) -> {
            int raycastDistance = PropulsionConfig.INLINE_OPTICAL_SENSOR_MAX_DISTANCE.get();
            return Component.translatable(payload.path + ".tooltip.summary").getString().replace("{}", String.valueOf(raycastDistance));
        });
    }

    private static class SummaryPayload {
        @SuppressWarnings("unused")
        public Item item;
        public String path;

        public SummaryPayload(Item item, String path) {
            this.item = item;
            this.path = path;
        }
    }
}
