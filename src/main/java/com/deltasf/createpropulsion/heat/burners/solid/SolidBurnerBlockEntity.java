package com.deltasf.createpropulsion.heat.burners.solid;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class SolidBurnerBlockEntity extends AbstractBurnerBlockEntity {
    private FuelInventoryBehaviour fuelInventory;
    private int burnTime = 0;

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

    public int getHeatPerTick() { return 1; }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();

        //Client-side prediction of state
        if (level.isClientSide()) {
            if (burnTime > 0) {
                burnTime--;
                heatSource.getCapability().ifPresent(cap -> cap.generateHeat(getHeatPerTick()));
            }
            return;
        }

        //Server-side m*th
        boolean wasBurning = burnTime > 0;

        //We are currently burning fuel
        if (burnTime > 0) {
            burnTime--;
            heatSource.getCapability().ifPresent(cap -> cap.generateHeat(getHeatPerTick())); 
        } else {
            //Thermostat check: we only consume fuel if we are not at max heat
            if (heatSource.getCapability().resolve().get().getHeatStored() < getBaseHeatCapacity()) {
                fuelInventory.tryConsumeFuel();
            }
        }

        updateBlockState(wasBurning);
    }

    @SuppressWarnings("null")
    private void updateBlockState(boolean wasBurning) {
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
            if (heat == 0) {
                return HeatLevel.NONE;
            }

            int maxHeat = cap.getMaxHeatStored();
            float percentage = (float) heat / maxHeat;

            if (percentage > 0.8f) {
                return HeatLevel.KINDLED;
            }
            if (percentage > 0.4f) {
                return HeatLevel.FADING;
            }

            return HeatLevel.SMOULDERING; 
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
        tag.putInt("BurnTime", burnTime);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        burnTime = tag.getInt("BurnTime");
    }
}
