package com.deltasf.createpropulsion.utility;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.thruster.FluidThrusterProperties;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;
import com.deltasf.createpropulsion.thruster.thruster.ThrusterBlockEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.simibubi.create.foundation.item.TooltipHelper.Palette;

import net.minecraft.network.chat.Component;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public class TooltipHandler {
    private static HashMap<Item, Function<SummaryPayload, String>> tooltipModificationLookup = new HashMap<Item, Function<SummaryPayload, String>>();

    @SubscribeEvent
    public static void addToItemTooltip(ItemTooltipEvent event) {
        //Looked this up in CDG
        Item item = event.getItemStack().getItem();
        String path = CreatePropulsion.ID + "." + ForgeRegistries.ITEMS.getKey(item).getPath();
        if (tooltipModificationLookup.isEmpty()) populateModifiables();

        List<Component> tooltip = event.getToolTip();
        List<Component> tooltipList = new ArrayList<>();
        //Add Create "Hold [Shift] for summary" for all items with tooltips defined in I18n. Only handles propulsion items
        if(ForgeRegistries.ITEMS.getKey(item).getNamespace().equals(CreatePropulsion.ID) && I18n.exists(path + ".tooltip.summary")) {
            handleI18nTooltip(tooltipList, item, path);
        }
        //Add Create "Hold [Shift] for fuel summary" for all fuels defined in ThrusterFuelManager. Handles items from other mods too
        ItemStack stack = event.getItemStack();
        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHandler -> {
            FluidStack fluidStack = fluidHandler.getFluidInTank(0);
            if (fluidStack == null || fluidStack.isEmpty()) return;

            Fluid fluid = fluidStack.getFluid();
            FluidThrusterProperties properties = ThrusterFuelManager.getProperties(fluid);
            if (properties == null) return;

            handleThrusterFuelTooltip(tooltipList, properties);
        });

        tooltip.addAll(1, tooltipList);
    }

    private static void handleI18nTooltip(List<Component> tooltipList, Item item, String path) {
        wrapShiftHoldText(tooltipList, "create.tooltip.holdForDescription", () -> {
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
        });
    }

    private static void handleThrusterFuelTooltip(List<Component> tooltipList, FluidThrusterProperties properties) {
        wrapShiftHoldText(tooltipList, "createpropulsion.tooltip.holdForRocketFuelSummary", () -> {
            //Thrust
            int thrustPercent = Math.round(properties.thrustMultiplier * 100.0f);
            Component thrustLine = Component.translatable("createpropulsion.tooltip.thrust")
                    .append(": ")
                    .withStyle(Palette.STANDARD_CREATE.primary())
                    .append(Component.literal(String.valueOf(thrustPercent))
                            .withStyle(Palette.STANDARD_CREATE.highlight()))
                    .append(Component.literal("%")
                            .withStyle(Palette.STANDARD_CREATE.primary()));
            tooltipList.add(thrustLine);

            //Burn rate
            int consumptionPercent = Math.round(properties.consumptionMultiplier * 100.0f);
            Component consumptionLine = Component.translatable("createpropulsion.tooltip.consumption")
                    .append(": ")
                    .withStyle(Palette.STANDARD_CREATE.primary())
                    .append(Component.literal(String.valueOf(consumptionPercent))
                            .withStyle(Palette.STANDARD_CREATE.highlight()))
                    .append(Component.literal("%")
                            .withStyle(Palette.STANDARD_CREATE.primary()));
            tooltipList.add(consumptionLine);
            
            tooltipList.add(Component.empty());
        });
    }


    private static void wrapShiftHoldText(List<Component> tooltipList, String langKey, Runnable addDetailedContent) {
        boolean isShiftDown = Screen.hasShiftDown();
        Component keyComponent = Component.translatable("create.tooltip.keyShift")
            .withStyle(isShiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY);

        tooltipList.add(Component.translatable(langKey, keyComponent).withStyle(ChatFormatting.DARK_GRAY));

        if (isShiftDown) {
            tooltipList.add(Component.empty());
            addDetailedContent.run();
        }
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
