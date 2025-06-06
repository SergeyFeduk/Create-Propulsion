package com.deltasf.createpropulsion.optical_sensors.optical_sensor;

import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class OpticalSensorDistanceValueBox extends ValueBoxTransform.Sided {
    private static final double FACING_OFFSET_VOXELS = -1.0;

    private Direction getPrimarySideDirection(BlockState state) {
        Direction facing = state.getValue(OpticalSensorBlock.FACING);
        if (facing.getAxis().isHorizontal()) {
            return facing.getClockWise(Axis.Y);
        } else {
            return Direction.EAST;
        }
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        Direction primarySide = getPrimarySideDirection(state);
        Direction oppositeSide = primarySide.getOpposite();

        return direction == primarySide || direction == oppositeSide;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8.0, 8.0, 14.5);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Vec3 location = super.getLocalOffset(level, pos, state);
        Direction facing = state.getValue(OpticalSensorBlock.FACING);
        double offsetAmount = FACING_OFFSET_VOXELS / 16.0;
        Vec3 facingOffset = Vec3.atLowerCornerOf(facing.getNormal()).scale(offsetAmount);
        location = location.add(facingOffset);
        return location;
    }
}
