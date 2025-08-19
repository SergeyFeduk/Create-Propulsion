package com.deltasf.createpropulsion.balloons.blocks;

import com.deltasf.createpropulsion.balloons.BalloonProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public class HabBlock extends Block {

    public HabBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide()) return;

        // Delegate the logic to our central processor.
        // For onPlace, 'state' is the newState and 'oldState' is the block that was replaced.
        //BalloonProcessor.processBlockChange(level, pos, oldState, state, isMoving);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        // Standard check to ensure we don't fire on simple block state changes.
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        
        if (!level.isClientSide()) {
            // Delegate the logic to our central processor.
            // For onRemove, 'state' is the oldState and 'newState' is what it's becoming.
            //BalloonProcessor.processBlockChange(level, pos, state, newState, isMoving);
        }
        
        // super.onRemove must be called last, as it removes the block entity.
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
