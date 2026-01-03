package com.deltasf.createpropulsion.thruster.creative_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CreativeThrusterValueBox extends ValueBoxTransform.Sided {

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 12.5f);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(CreativeThrusterBlock.FACING);
        return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(3 / 16.0));
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        super.rotate(level, pos, state, ms);

        Direction facing = state.getValue(CreativeThrusterBlock.FACING);
    
        if (getSide().getAxis() == Axis.Y) {
            TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        Direction thrusterFacing = state.getValue(CreativeThrusterBlock.FACING);
        Direction placementFacing = state.getValue(CreativeThrusterBlock.PLACEMENT_FACING);

        if (side.getAxis() == thrusterFacing.getAxis()) {
            return false;
        }

        if (side == placementFacing) {
            return false;
        }

        return true;
    }
}