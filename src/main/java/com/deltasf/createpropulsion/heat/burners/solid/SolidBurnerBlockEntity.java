package com.deltasf.createpropulsion.heat.burners.solid;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.deltasf.createpropulsion.heat.IHeatSource;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class SolidBurnerBlockEntity extends AbstractBurnerBlockEntity {
    private FuelInventoryBehaviour fuelInventory;

    public SolidBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        fuelInventory = new FuelInventoryBehaviour(this);
        behaviours.add(fuelInventory);
    }

    @Override
    public void tick() {
        //If we are ticking a fuel - update the timer and produce HU, but only if HU is less than max value

        //if we are not ticking fuel & there is fuel - set a burn timer and consume 1 item from fuel stack
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return fuelInventory.getCapability(cap);
        return super.getCapability(cap, side);
    }

    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }

    @Override
    protected int getBaseHeatCapacity() {
        return 400;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        IHeatSource heatSourceCap = heatSource.getCapability().resolve().get();
        if (heatSourceCap == null) return false;


        Lang.builder()
            .add(Lang.number(heatSourceCap.getHeatStored()))
            .text(" / ")
            .add(Lang.number(heatSourceCap.getMaxHeatStored())).forGoggles(tooltip);

        Lang.builder().add(fuelInventory.fuelStack.getDisplayName()).forGoggles(tooltip);

        String t = String.valueOf(fuelInventory.fuelStack.getCount());
        Lang.builder().text(t).forGoggles(tooltip);
        
        return true;
    }
}
