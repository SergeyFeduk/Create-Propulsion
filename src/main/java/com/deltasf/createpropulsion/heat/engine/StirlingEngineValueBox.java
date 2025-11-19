package com.deltasf.createpropulsion.heat.engine;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class StirlingEngineValueBox extends ValueBoxTransform.Sided {

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 12.5, 7);
    }

    @Override
    public Vec3 getLocalOffset(BlockState state) {
        Direction facing = state.getValue(StirlingEngineBlock.FACING);
        Vec3 location = getSouthLocation();
        location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(facing), Axis.Y);
        
        return location;
    }

    @Override
    public void rotate(BlockState state, PoseStack ms) {
        Direction facing = state.getValue(StirlingEngineBlock.FACING);
        float yRot = AngleHelper.horizontalAngle(facing);
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
        ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return direction == Direction.UP;
    }
}