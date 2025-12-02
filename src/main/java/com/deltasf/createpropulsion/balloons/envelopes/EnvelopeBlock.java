package com.deltasf.createpropulsion.balloons.envelopes;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EnvelopeBlock extends AbstractEnvelopeBlock {
    public EnvelopeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        return EnvelopeLogic.tryDye(state, level, pos, player, hand, false);
    }
}
