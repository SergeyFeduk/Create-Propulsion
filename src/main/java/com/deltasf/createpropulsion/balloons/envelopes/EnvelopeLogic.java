package com.deltasf.createpropulsion.balloons.envelopes;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.registries.PropulsionBlocks.EnvelopeColor;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EnvelopeLogic {

    private static final Map<Item, EnvelopeColor> DYE_TO_COLOR = new HashMap<>();

    static {
        for (EnvelopeColor color : EnvelopeColor.values()) {
            if (color.getDye() != null) {
                DYE_TO_COLOR.put(color.getDye(), color);
            }
        }
    }

    public static void onPlace(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!level.isClientSide()) {
            BalloonShipRegistry.updater().habBlockPlaced(pos, level);
        }
    }

    public static boolean onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState) {
        if (state.is(newState.getBlock())) {
            return false;
        }
        
        if (!level.isClientSide()) {
            BalloonShipRegistry.updater().habBlockRemoved(pos, level);
        }
        return true;
    }

    public static InteractionResult tryDye(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, boolean isShaft) {
        if (player.isSpectator()) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        EnvelopeColor targetColor = DYE_TO_COLOR.get(heldItem.getItem());

        if (targetColor == null) return InteractionResult.PASS; //Huh

        Block targetBlock = isShaft ? PropulsionBlocks.getEnvelopedShaft(targetColor).get() : PropulsionBlocks.getEnvelope(targetColor).get();
        // Do not waste dye on the same color
        if (state.is(targetBlock)) return InteractionResult.PASS;

        BlockState newState = targetBlock.defaultBlockState();
        if (isShaft && state.hasProperty(RotatedPillarKineticBlock.AXIS)) {
            newState = newState.setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS));
        }

        // Apply changes
        if (!player.isCreative()) heldItem.shrink(1);
        level.setBlock(pos, newState, 3);
        
        // Effects
        level.playLocalSound(pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1, 1, false);
        if (level.isClientSide) {
            for(int i = 0; i < 20; i++) {
                double dx = pos.getX() + level.random.nextDouble();
                double dy = pos.getY() + level.random.nextDouble();
                double dz = pos.getZ() + level.random.nextDouble();
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, newState), dx, dy, dz, 0.0, 0.0, 0.0);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
