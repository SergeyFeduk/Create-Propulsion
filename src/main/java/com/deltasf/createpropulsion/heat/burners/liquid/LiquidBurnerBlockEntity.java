package com.deltasf.createpropulsion.heat.burners.liquid;

import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.heat.HeatMapper.HeatLevelString;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.burners.BurnerDamager;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LiquidBurnerBlockEntity extends AbstractBurnerBlockEntity {
    private HeatLevelString heatLevelName = HeatLevelString.COLD;
    private boolean isPowered = false;
    private BurnerDamager damager;

    protected SmartFluidTankBehaviour tank;
    private final Map<Direction, LazyOptional<IFluidHandler>> fluidCaps = new IdentityHashMap<>();

    private static final float MAX_HEAT = 400.0f;

    public LiquidBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        
        damager = new BurnerDamager(this);
        behaviours.add(damager);

        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    protected Direction.Axis getPipeAxis() {
        if (getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) {
            return getBlockState().getValue(HorizontalDirectionalBlock.FACING).getAxis();
        }
        return Direction.Axis.X;
    }

    public void updatePipeCapability() {
        fluidCaps.values().forEach(LazyOptional::invalidate);
        fluidCaps.clear();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        fluidCaps.values().forEach(LazyOptional::invalidate);
    }

    public float getHeatPerTick() { return 2; }

    @SuppressWarnings("null")
    public void updatePoweredState() {
        if (level == null || level.isClientSide()) return;
        boolean currentlyPowered = level.getBestNeighborSignal(worldPosition) > 0;
        if (this.isPowered != currentlyPowered) {
            this.isPowered = currentlyPowered;
            notifyUpdate();
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }

    @Override
    protected float getBaseHeatCapacity() { return MAX_HEAT; }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && side != null) {
            if (side.getAxis() == getPipeAxis()) {
                return fluidCaps.computeIfAbsent(side, s -> LazyOptional.of(() -> new PassthroughFluidHandler(s))).cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        /*if (!heatSource.getCapability().isPresent()) return false;
        IHeatSource heatSourceCap = heatSource.getCapability().resolve().get();
        
        CreateLang.builder()
            .add(CreateLang.number(heatSourceCap.getHeatStored()))
            .text(" / ")
            .add(CreateLang.number(heatSourceCap.getMaxHeatStored())).forGoggles(tooltip);*/

        ChatFormatting color = null;
        String key = null;

        switch (heatLevelName) {
            case COLD:
                color = ChatFormatting.BLUE;
                key = "gui.goggles.burner.heat.cold";
                break;
            case WARM:
                color = ChatFormatting.GOLD;
                key = "gui.goggles.burner.heat.warm";
                break;
            case HOT:
                color = ChatFormatting.GOLD;
                key = "gui.goggles.burner.heat.hot";
                break;
            case SEARING:
                color = ChatFormatting.RED;
                key = "gui.goggles.burner.heat.searing";
                break;
            default:
                color = ChatFormatting.BLUE;
                break;
        }

        //Heat level
        CreateLang.builder().add(CreateLang.translate("gui.goggles.burner.status")).text(": ").add(CreateLang.translate(key).style(color)).forGoggles(tooltip);

        //Thermostat on/off
        CreateLang.builder()
            .add(CreateLang.translate("gui.goggles.burner.thermostat"))
            .text(": ")
            .add(CreateLang.translate(!isPowered ? "gui.goggles.burner.thermostat.on" : "gui.goggles.burner.thermostat.off")
                .style(!isPowered ? ChatFormatting.GREEN : ChatFormatting.RED))
            .forGoggles(tooltip);

        //Fuel
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability().cast());
        return true;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putString("heatLevelName", heatLevelName.name());
        tag.putBoolean("isPowered", isPowered); 
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        heatLevelName = HeatLevelString.valueOf(tag.getString("heatLevelName"));
        isPowered = tag.getBoolean("isPowered");
    }

    private class PassthroughFluidHandler implements IFluidHandler {
        private final Direction inputSide; 

        public PassthroughFluidHandler(Direction side) {
            this.inputSide = side;
        }

        private boolean isFuel(FluidStack stack) {
            // TODO: 
            return stack.getFluid().isSame(Fluids.WATER);
        }

        @SuppressWarnings("null")
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;

            int totalFilled = 0;
            FluidStack remaining = resource.copy();

            // Try to fill internal tank
            if (isFuel(resource)) {
                int filledIntoTank = tank.getPrimaryHandler().fill(remaining, action);
                totalFilled += filledIntoTank;
                remaining.shrink(filledIntoTank);
            }

            // Pass to the opposite side
            if (!remaining.isEmpty()) {
                Direction outputSide = inputSide.getOpposite();
                BlockPos neighborPos = worldPosition.relative(outputSide);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);

                if (neighbor != null) {
                    LazyOptional<IFluidHandler> neighborCap = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, inputSide);
                    IFluidHandler cap = neighborCap.orElse(null);
                    if (cap != null) {
                        int filledIntoNeighbor = cap.fill(remaining, action);
                        totalFilled += filledIntoNeighbor;
                    }
                }
            }

            return totalFilled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.getPrimaryHandler().drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.getPrimaryHandler().drain(maxDrain, action);
        }

        @Override
        public int getTanks() { return tank.getPrimaryHandler().getTanks(); }

        @Override
        public FluidStack getFluidInTank(int tankIndex) { return tank.getPrimaryHandler().getFluidInTank(tankIndex); }

        @Override
        public int getTankCapacity(int tankIndex) { return tank.getPrimaryHandler().getTankCapacity(tankIndex); }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) { return tank.getPrimaryHandler().isFluidValid(tankIndex, stack); }
    }
}