package com.deltasf.createpropulsion.balloons.envelopes;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("deprecation")
public class EnvelopeBlock extends Block implements IEnvelope {
    public EnvelopeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        EnvelopeLogic.onPlace(level, pos);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        EnvelopeLogic.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean isEnvelope() { return true; }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        return EnvelopeLogic.tryDye(state, level, pos, player, hand, false);
    }
}
