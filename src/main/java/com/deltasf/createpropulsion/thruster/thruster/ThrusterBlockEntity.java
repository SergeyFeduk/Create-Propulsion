package com.deltasf.createpropulsion.thruster.thruster;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.thruster.AbstractThrusterBlockEntity;
import com.deltasf.createpropulsion.thruster.FluidThrusterProperties;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

import javax.annotation.Nullable;

public class ThrusterBlockEntity extends AbstractThrusterBlockEntity {
    public static final float BASE_FUEL_CONSUMPTION = 2;
    public static final int BASE_MAX_THRUST = 600000;
    public SmartFluidTankBehaviour tank;

    public ThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && side == getFluidCapSide()) {
            return tank.getCapability().cast();
        }
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour.isPeripheralCap(cap)) {
            return computerBehaviour.getPeripheralCapability().cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        float thrust = 0;
        int power = getOverriddenPowerOrState(currentBlockState);

        // This thruster only works if it has valid fuel and power
        if (isWorking() && power > 0) {
            var properties = getFuelProperties(fluidStack().getRawFluid());
            float powerPercentage = power / 15.0f;
            float obstructionEffect = calculateObstructionEffect();
            float thrustPercentage = Math.min(powerPercentage, obstructionEffect);

            if (thrustPercentage > 0 && properties != null) {
                int tick_rate = PropulsionConfig.THRUSTER_TICKS_PER_UPDATE.get();
                int consumption = calculateFuelConsumption(powerPercentage, properties.consumptionMultiplier, tick_rate);
                FluidStack drainedStack = tank.getPrimaryHandler().drain(consumption, IFluidHandler.FluidAction.EXECUTE);
                int fuelConsumed = drainedStack.getAmount();

                if (fuelConsumed > 0) {
                    float consumptionRatio = (float) fuelConsumed / (float) consumption;

                    float thrustMultiplier = (float) (double) PropulsionConfig.THRUSTER_THRUST_MULTIPLIER.get();
                    thrust = BASE_MAX_THRUST * thrustMultiplier * thrustPercentage * properties.thrustMultiplier * consumptionRatio;
                }
            }
        }
        thrusterData.setThrust(thrust);
        isThrustDirty = false;
    }

    @Override
    protected boolean isWorking() {
        return validFluid();
    }

    protected Direction getFluidCapSide() {
        return getBlockState().getValue(ThrusterBlock.FACING);
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (fluidStack().isEmpty()) {
            return CreateLang.translate("gui.goggles.thruster.status.no_fuel").style(ChatFormatting.RED);
        } else if (!validFluid()) {
            return CreateLang.translate("gui.goggles.thruster.status.wrong_fuel").style(ChatFormatting.RED);
        } else if (!isPowered()) {
            return CreateLang.translate("gui.goggles.thruster.status.not_powered").style(ChatFormatting.GOLD);
        } else if (emptyBlocks == 0) {
            return CreateLang.translate("gui.goggles.thruster.obstructed").style(ChatFormatting.RED);
        } else {
            return CreateLang.translate("gui.goggles.thruster.status.working").style(ChatFormatting.GREEN);
        }
    }

    @Override
    protected void addSpecificGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability().cast());
    }

    public FluidStack fluidStack() {
        return tank.getPrimaryHandler().getFluid();
    }

    public boolean validFluid() {
        if (fluidStack().isEmpty()) return false;
        return getFuelProperties(fluidStack().getRawFluid()) != null;
    }

    public FluidThrusterProperties getFuelProperties(Fluid fluid) {
        return ThrusterFuelManager.getProperties(fluid);
    }

    private int calculateFuelConsumption(float powerPercentage, float fluidPropertiesConsumptionMultiplier, int tick_rate) {
        float base_consumption = BASE_FUEL_CONSUMPTION * (float) (double) PropulsionConfig.THRUSTER_CONSUMPTION_MULTIPLIER.get();
        return (int) Math.ceil(base_consumption * powerPercentage * fluidPropertiesConsumptionMultiplier * tick_rate);
    }
}
