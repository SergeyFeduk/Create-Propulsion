package com.deltasf.createpropulsion.balloons.envelopes;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.registries.PropulsionBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EnvelopeBlock extends AbstractEnvelopeBlock {
    public EnvelopeBlock(Properties properties) {
        super(properties);
    }

    private static final Map<Item, BlockState> dyes = new HashMap<>();

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (player.isSpectator())
            return InteractionResult.PASS;
        
        ItemStack heldItem = player.getItemInHand(hand);
        Item item = heldItem.getItem();
        if (dyes.isEmpty()) populateDyes();

        if (dyes.containsKey(item)) {
            BlockState blockState = dyes.get(item);
            if (blockState.getBlock() == level.getBlockState(pos).getBlock()) return InteractionResult.PASS; //Do not waste dye on the same color
            if (!player.isCreative()) heldItem.shrink(1); //Consume dye
            level.setBlock(pos, blockState, 3); //Replace block
            //Effects
            level.playLocalSound(pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1, 1, false);
            if (level.isClientSide) {
                for(int i = 0; i < 20; i++) {
                    double dx = pos.getX() + level.random.nextDouble();
                    double dy = pos.getY() + level.random.nextDouble();
                    double dz = pos.getZ() + level.random.nextDouble();
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState), dx, dy, dz, 0.0, 0.0, 0.0);
                }
            }
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }

    private void populateDyes() {
        for(PropulsionBlocks.EnvelopeColor color : PropulsionBlocks.EnvelopeColor.values()) {
            Item dye = color.getDye();
            if (dye == null) continue;
            dyes.put(dye, PropulsionBlocks.getEnvelope(color).getDefaultState());
        }
    }
}
