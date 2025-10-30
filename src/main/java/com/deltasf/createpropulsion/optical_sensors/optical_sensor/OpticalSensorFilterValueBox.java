package com.deltasf.createpropulsion.optical_sensors.optical_sensor;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;

public class OpticalSensorFilterValueBox extends ValueBoxTransform.Sided {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final boolean primaryIsRelativeTop;
    private static final double FACING_OFFSET_VOXELS = -2.0;

    public OpticalSensorFilterValueBox(boolean primaryIsRelativeTop) {
        this.primaryIsRelativeTop = primaryIsRelativeTop;
    }

    private Direction getPrimaryDirection(BlockState state) {
        if (!state.hasProperty(FACING)) {
            // Fallback if FACING is missing because of bit flip due to cosmic rays (must account for that)
            System.err.println("Warning: BlockState missing FACING property for OrientableTopBottomValueBoxTransform!");
            return primaryIsRelativeTop ? Direction.UP : Direction.DOWN;
        }
        Direction facing = state.getValue(FACING);
        if (facing.getAxis().isHorizontal()) {
            return primaryIsRelativeTop ? Direction.UP : Direction.DOWN;
        } else if (facing == Direction.UP) {
            return primaryIsRelativeTop ? Direction.NORTH : Direction.SOUTH;
        } else {
            return primaryIsRelativeTop ? Direction.NORTH : Direction.SOUTH;
        }
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        Direction primary = getPrimaryDirection(state);
        Direction opposite = primary.getOpposite();
        return direction == primary || direction == opposite;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8.0, 8.0, 15.5);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Vec3 location = super.getLocalOffset(level, pos, state);
        Direction facing = state.getValue(FACING);
        double offsetAmount = FACING_OFFSET_VOXELS / 16.0;
        Vec3 facingOffset = Vec3.atLowerCornerOf(facing.getNormal()).scale(offsetAmount);
        location = location.add(facingOffset);
        return location;
    }
}