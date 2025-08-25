package com.deltasf.createpropulsion.balloons.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.balloons.registries.BalloonProcessor;

@SuppressWarnings("deprecation")
public class HabBlock extends Block {

    public HabBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide()) return;

        BalloonProcessor.processBlockPlacement(level, pos);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        
        if (!level.isClientSide()) {
            //BalloonProcessor.processBlockChange(level, pos, state, newState, isMoving);
        }
        
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
