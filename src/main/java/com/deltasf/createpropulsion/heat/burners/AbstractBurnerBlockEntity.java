package com.deltasf.createpropulsion.heat.burners;

import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.deltasf.createpropulsion.heat.HeatSourceBehavior;
import com.deltasf.createpropulsion.heat.IHeatSource;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractBurnerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    protected HeatSourceBehavior heatSource;

    public AbstractBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        heatSource = new HeatSourceBehavior(this, getBaseHeatCapacity());
        behaviours.add(heatSource);
    }

    protected abstract int getBaseHeatCapacity();

    protected abstract Direction getHeatCapSide();

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PropulsionCapabilities.HEAT_SOURCE && side == getHeatCapSide()) {
            return heatSource.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        heatSource.getCapability().invalidate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        IHeatSource heatSourceCap = heatSource.getCapability().resolve().get();
        if (heatSourceCap == null) return false;


        Lang.builder()
            .add(Lang.number(heatSourceCap.getHeatStored()))
            .text(" / ")
            .add(Lang.number(heatSourceCap.getMaxHeatStored())).forGoggles(tooltip);

        return true;
    }
}
