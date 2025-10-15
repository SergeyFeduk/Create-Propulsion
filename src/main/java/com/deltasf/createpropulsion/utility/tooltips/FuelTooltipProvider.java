package com.deltasf.createpropulsion.utility.tooltips;

import java.util.List;

import com.deltasf.createpropulsion.thruster.FluidThrusterProperties;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.FluidStack;

public class FuelTooltipProvider implements ITooltipProvider {
    @Override
    public void addTooltip(ItemTooltipEvent event, List<Component> tooltipList) {
        ItemStack stack = event.getItemStack();

        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(fluidHandler -> {
            FluidStack fluidStack = fluidHandler.getFluidInTank(0);
            if (fluidStack == null || fluidStack.isEmpty()) return;

            FluidThrusterProperties properties = ThrusterFuelManager.getProperties(fluidStack.getFluid());
            if (properties == null) return;

            TooltipHandler.wrapShiftHoldText(tooltipList, "createpropulsion.tooltip.holdForRocketFuelSummary", () -> {
                // Thrust
                int thrustPercent = Math.round(properties.thrustMultiplier * 100.0f);
                Component thrustLine = Component.translatable("createpropulsion.tooltip.thrust")
                    .append(": ")
                    .withStyle(Palette.STANDARD_CREATE.primary())
                    .append(Component.literal(String.valueOf(thrustPercent)).withStyle(Palette.STANDARD_CREATE.highlight()))
                    .append(Component.literal("%").withStyle(Palette.STANDARD_CREATE.primary()));
                tooltipList.add(thrustLine);

                // Burn rate
                int consumptionPercent = Math.round(properties.consumptionMultiplier * 100.0f);
                Component consumptionLine = Component.translatable("createpropulsion.tooltip.consumption")
                    .append(": ")
                    .withStyle(Palette.STANDARD_CREATE.primary())
                    .append(Component.literal(String.valueOf(consumptionPercent)).withStyle(Palette.STANDARD_CREATE.highlight()))
                    .append(Component.literal("%").withStyle(Palette.STANDARD_CREATE.primary()));
                tooltipList.add(consumptionLine);
                
                tooltipList.add(Component.empty());
            });
        });

    }
}
