package com.deltasf.createpropulsion.lodestone_tracker;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class LodestoneTrackerBlock extends Block implements EntityBlock {
    public LodestoneTrackerBlock(Properties properties){
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LodestoneTrackerBlockEntity(CreatePropulsion.LODESTONE_TRACKER_BLOCK_ENTITY.get(), pos, state);
    }

    //Handling hand interactions
    @SuppressWarnings("null")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        if (ray.getDirection() != Direction.UP) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LodestoneTrackerBlockEntity trackerBE)) return InteractionResult.PASS;

        ItemStack heldStack = player.getItemInHand(hand);
        boolean trackerHasCompass = trackerBE.hasCompass();
        boolean playerHoldingCompass = heldStack.getItem() == Items.COMPASS;

        if (playerHoldingCompass && !trackerHasCompass) {
            // Player has compass, tracker is empty -> Place compass
            ItemStack compassToPlace = heldStack.split(1);
            trackerBE.setCompass(compassToPlace);
            AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
            return InteractionResult.CONSUME;

        } else if (heldStack.isEmpty() && trackerHasCompass) {
            // Player has empty hand, tracker has compass -> Take compass
            ItemStack takenCompass = trackerBE.removeCompass();
            player.getInventory().placeItemBackInInventory(takenCompass);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return new SmartBlockEntityTicker<>();
    }
}
