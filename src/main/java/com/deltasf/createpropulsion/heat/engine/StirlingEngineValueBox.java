package com.deltasf.createpropulsion.heat.engine;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class StirlingEngineValueBox extends ValueBoxTransform.Sided {

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 12.51, 7);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(StirlingEngineBlock.HORIZONTAL_FACING);
        Vec3 location = getSouthLocation();
        location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(facing), Axis.Y);
        
        return location;
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        Direction facing = state.getValue(StirlingEngineBlock.HORIZONTAL_FACING);
        float yRot = AngleHelper.horizontalAngle(facing);
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
        ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return direction == Direction.UP;
    }
}
