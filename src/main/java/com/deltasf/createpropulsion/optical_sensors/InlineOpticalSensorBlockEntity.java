package com.deltasf.createpropulsion.optical_sensors;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class InlineOpticalSensorBlockEntity extends AbstractOpticalSensorBlockEntity {
    public InlineOpticalSensorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) { }

    @Override
    public float getZAxisOffset() {
        return -0.125f;
    }

    @Override
    protected float getMaxRaycastDistance() {
        return PropulsionConfig.INLINE_OPTICAL_SENSOR_MAX_DISTANCE.get();
    }

    @Override
    protected void updateRedstoneSignal(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockPos pos, int rawNewPower, @Nullable BlockPos hitBlockPos) {
        int finalPower = rawNewPower;
        int oldPower = state.getValue(AbstractOpticalSensorBlock.POWER);

        if (oldPower != finalPower) {
            BlockState updatedState = state.setValue(AbstractOpticalSensorBlock.POWER, finalPower).setValue(AbstractOpticalSensorBlock.POWERED, finalPower > 0);
            level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);

            Direction facingDir = state.getValue(AbstractOpticalSensorBlock.FACING);
            level.updateNeighborsAt(pos.relative(facingDir.getOpposite()), state.getBlock());
        }
    }
}
