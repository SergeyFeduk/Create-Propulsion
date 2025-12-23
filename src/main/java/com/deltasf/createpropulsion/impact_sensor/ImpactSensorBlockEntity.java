package com.deltasf.createpropulsion.impact_sensor;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ImpactSensorBlockEntity extends BlockEntity implements BlockEntityPhysicsListener {
    private String dimension;

    public ImpactSensorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void physTick(@Nullable PhysShip ship, @NotNull PhysLevel level) {

    }

    @Override
    @NotNull
    public String getDimension() {
        return dimension;
    }

    @Override
    public void setDimension(@NotNull String dimension) {
        this.dimension = dimension;
    }
}
