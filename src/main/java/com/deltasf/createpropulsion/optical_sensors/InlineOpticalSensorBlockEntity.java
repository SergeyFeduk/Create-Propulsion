package com.deltasf.createpropulsion.optical_sensors;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.PropulsionItems;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class InlineOpticalSensorBlockEntity extends AbstractOpticalSensorBlockEntity {
    private static final int LENS_LIMIT = 1;
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
    public int getLensLimit() { return LENS_LIMIT; }

    @Override
    protected float getMaxRaycastDistance() {
        boolean isFocused = hasLens(PropulsionItems.FOCUS_LENS.get());
        return PropulsionConfig.INLINE_OPTICAL_SENSOR_MAX_DISTANCE.get() * (isFocused ? 2.0f : 1.0f);
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
