package com.deltasf.createpropulsion.balloons.envelopes;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("deprecation")
public abstract class AbstractEnvelopeBlock extends Block implements IEnvelope {
    public AbstractEnvelopeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            BalloonShipRegistry.updater().habBlockPlaced(pos, level);
        }
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        
        if (!level.isClientSide()) {
            BalloonShipRegistry.updater().habBlockRemoved(pos, level);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean isEnvelope() { return true; }
}
