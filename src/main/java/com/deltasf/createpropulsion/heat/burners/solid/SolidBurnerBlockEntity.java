package com.deltasf.createpropulsion.heat.burners.solid;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.deltasf.createpropulsion.heat.IHeatSource;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.burners.HeatToHeatLevelMapping;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
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

    private static final float MAX_HEAT = 400.0f;
    private static final float PASSIVE_LOSS_PER_TICK = 0.05f;

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

    public float getHeatPerTick() { return 1; }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide()) return;

        //Calculate rates
        float heatGeneration = (burnTime > 0) ? getHeatPerTick() : 0;
        if (burnTime > 0) burnTime--;

        float heatConsumedLastTick = offerHeatToConsumer();
        float passiveLoss = 0;
        if (heatConsumedLastTick == 0) {
            heatConsumedLastTick = heatSource.getCapability().map(cap -> cap.getHeatStored() > 0 ? PASSIVE_LOSS_PER_TICK : 0f).orElse(0f);
        }

        //Apply heat changes
        float netHeatChange = heatGeneration - passiveLoss - heatConsumedLastTick;
        heatSource.getCapability().ifPresent(cap -> {
            if (netHeatChange > 0) cap.generateHeat(netHeatChange);
            else cap.extractHeat(Math.abs(netHeatChange), false);
        });

        //Thermostat
        boolean refueled = false;
        if (burnTime <= 0) {
            refueled = needsRefuel();
            if (refueled) {
                fuelInventory.tryConsumeFuel();
            }
        }

        //Sync and update state
        if (refueled) {
            notifyUpdate();
        }

        updateBlockState();
    }

    @SuppressWarnings("null")
    private boolean needsRefuel() {
        if (level == null) return false;
        BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
        if (beAbove == null) return false;

        return beAbove.getCapability(PropulsionCapabilities.HEAT_CONSUMER, Direction.DOWN).map(consumer -> {
            if (!consumer.isActive()) {
                return false;
            }

            //Get minimum heat required by consumer
            float thresholdPercent = consumer.getOperatingThreshold();
            float thresholdInHU = MAX_HEAT * thresholdPercent;

            //Predict heat on next tick
            float currentHeat = heatSource.getCapability().map(IHeatSource::getHeatStored).orElse(0f);
            float consumptionNextTick = consumer.consumeHeat(currentHeat, true);
            float heatNextTick = currentHeat - consumptionNextTick - PASSIVE_LOSS_PER_TICK;

            return heatNextTick < thresholdInHU;
        }).orElse(false);
    }

    @SuppressWarnings("null")
    private float offerHeatToConsumer() {
        if (level == null) return 0f;
        BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
        if (beAbove == null) return 0f;

        return beAbove.getCapability(PropulsionCapabilities.HEAT_CONSUMER, Direction.DOWN).map(consumer -> {
            float availableHeat = heatSource.getCapability().map(IHeatSource::getHeatStored).orElse(0f);
            if (availableHeat <= 0) return 0f;

            // The consumer decides how much heat to pull
            float thresholdPercent = consumer.getOperatingThreshold();
            float thresholdInHU = MAX_HEAT * thresholdPercent;

            if (consumer.isActive() && availableHeat >= thresholdInHU) {
                return consumer.consumeHeat(availableHeat, false);
            }

            return 0f;
        }).orElse(0f);
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
            if (cap.getHeatStored() == 0) return HeatLevel.NONE;
            float percentage = cap.getHeatStored() / cap.getMaxHeatStored();
            return HeatToHeatLevelMapping.getHeatLevel(percentage);
        }).orElse(HeatLevel.NONE);
    }

    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }

    @Override
    protected float getBaseHeatCapacity() {
        return MAX_HEAT;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        //TODO: Figure out how to make server notify us of update every tick ONLY when we are looking at goggle tooltip
        //For now I'll just update this when state changes and display "artistic name" for heat level, not actual number
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
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        burnTime = tag.getInt("burnTime");
    }
}
