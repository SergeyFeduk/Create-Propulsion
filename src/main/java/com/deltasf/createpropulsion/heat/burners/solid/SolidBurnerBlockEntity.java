package com.deltasf.createpropulsion.heat.burners.solid;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class SolidBurnerBlockEntity extends AbstractBurnerBlockEntity {
    private FuelInventoryBehaviour fuelInventory;
    private int burnTime = 0;
    private int activeConsumptionRate = 0;
    private int passiveLossTimer = 0;

    private static final int PASSIVE_LOSS_INTERVAL = 10;

    public SolidBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        fuelInventory = new FuelInventoryBehaviour(this);
        behaviours.add(fuelInventory);
    }

    public ItemStack getFuelStack() {
        return fuelInventory.fuelStack;
    }

    public int getHeatPerTick() { return 1; }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();

        simulateTick();
        //Client only needs to simulate its own tick to predict the actual state
        if (level.isClientSide()) {
            return;
        }

        //Calculate rates
        int oldRate = this.activeConsumptionRate;
        this.activeConsumptionRate = calculateCurrentConsumption();
        boolean consumptionChanged = oldRate != this.activeConsumptionRate;

        //Refuel if needed
        boolean refueled = false;
        if (burnTime <= 0) {
            refueled = heatSource.getCapability().map(cap -> {
                if (cap.getHeatStored() < cap.getMaxHeatStored()) {
                    return fuelInventory.tryConsumeFuel();
                }
                return false;
            }).orElse(false);
        }
        //Sync clients on unexpected state change
        if (refueled || consumptionChanged) {
            notifyUpdate();
        }

        updateBlockState();
    }

    private void simulateTick() {
        int heatGeneration = (burnTime > 0) ? getHeatPerTick() : 0;

        if (burnTime > 0) {
            burnTime--;
        }

        //Passive loss - loses 1 HU every PASSIVE_LOSS_INTERVAL ticks
        int passiveLoss = 0;
        if (activeConsumptionRate == 0 && heatSource.getCapability().map(cap -> cap.getHeatStored() > 0).orElse(false)) {
            passiveLossTimer++;
            if (passiveLossTimer >= PASSIVE_LOSS_INTERVAL) {
                passiveLoss = 1;
                passiveLossTimer = 0;
            }
        } else {
            passiveLossTimer = 0;
        }

        //Heat change due to generation, consumption or passive loss
        int netHeatChange = heatGeneration - activeConsumptionRate - passiveLoss;
        if (netHeatChange == 0) return;

        heatSource.getCapability().ifPresent(cap -> {
            if (netHeatChange > 0) {
                cap.generateHeat(netHeatChange);
            } else {
                cap.extractHeat(Math.abs(netHeatChange), false);
            }
        });
    }

    @SuppressWarnings("null")
    private int calculateCurrentConsumption() {
        //TODO: Get rid of this. We should add HEAT_CONSUMER capability and use it for determining heat consumption instead
        if (level == null) return 0;
        BlockPos consumerPos = worldPosition.above();
        BlockEntity consumerBlockEntity = level.getBlockEntity(consumerPos);

        //Boiler check
        if (consumerBlockEntity instanceof FluidTankBlockEntity tank) {
            if (tank.boiler.isActive() || tank.boiler.isPassive()) {
                if (tank.boiler.activeHeat > 0 || tank.boiler.passiveHeat) return 1;
            }
            return 0;
        }
        //Basin check
        if (consumerBlockEntity instanceof BasinBlockEntity) {
            if (level.getBlockEntity(consumerPos.above(2)) instanceof MechanicalMixerBlockEntity mixer) {
                if (mixer.running) return 1;
            }
            return 0;
        }

        //TODO: Impl hot air pump check
        return 0;
    }

    @SuppressWarnings("null")
    private void updateBlockState() {
        boolean isBurningNow = burnTime > 0;
        HeatLevel currentHeatLevel = calculateHeatLevel();

        if (getBlockState().getValue(SolidBurnerBlock.LIT) != isBurningNow || getBlockState().getValue(AbstractBurnerBlock.HEAT) != currentHeatLevel) {
            level.setBlock(worldPosition, getBlockState()
                .setValue(SolidBurnerBlock.LIT, isBurningNow)
                .setValue(AbstractBurnerBlock.HEAT, currentHeatLevel), 3);
        }
    }

    private HeatLevel calculateHeatLevel() {
        return heatSource.getCapability().map(cap -> {
            int heat = cap.getHeatStored();
            if (heat == 0) return HeatLevel.NONE;
            int maxHeat = cap.getMaxHeatStored();
            float percentage = (float) heat / maxHeat;
            if (percentage > 0.6f) return HeatLevel.KINDLED;
            if (percentage > 0.3f) return HeatLevel.FADING;
            return HeatLevel.NONE; 
        }).orElse(HeatLevel.NONE);
    }

    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }

    @Override
    protected int getBaseHeatCapacity() {
        return 400;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        heatSource.getCapability().ifPresent(cap -> {
            Lang.builder()
                .text("Heat: ")
                .add(Lang.number(cap.getHeatStored()))
                .text(" / ")
                .add(Lang.number(cap.getMaxHeatStored()))
                .text(" HU")
                .forGoggles(tooltip);
        });

        if (fuelInventory != null) {
            ItemStack fuel = fuelInventory.fuelStack;
            if (!fuel.isEmpty()) {
                Lang.builder().text("Fuel: ").add(fuel.getDisplayName()).forGoggles(tooltip);
                String t = "Amount: " + fuel.getCount();
                Lang.builder().text(t).forGoggles(tooltip);
            } else {
                Lang.builder().text("Empty").forGoggles(tooltip);
            }
        }
        
        return true;

    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return fuelInventory.getCapability(cap);
        return super.getCapability(cap, side);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("burnTime", burnTime);
        tag.putInt("activeConsumptionRate", activeConsumptionRate);
        if (!clientPacket) {
            tag.putInt("passiveLossTimer", passiveLossTimer);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        burnTime = tag.getInt("burnTime");
        activeConsumptionRate = tag.getInt("activeConsumptionRate");
        if (!clientPacket) {
            passiveLossTimer = tag.getInt("passiveLossTimer");
        }
    }
}
