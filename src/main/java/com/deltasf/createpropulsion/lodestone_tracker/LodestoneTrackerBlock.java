package com.deltasf.createpropulsion.lodestone_tracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class LodestoneTrackerBlock extends Block {
    public LodestoneTrackerBlock(Properties properties){
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        if (ray.getDirection() != Direction.UP) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        //Beh check

        ItemStack heldStack = player.getItemInHand(hand);
        boolean wasEmptyHanded = heldStack.isEmpty();
        boolean isCompass = wasEmptyHanded ? false : heldStack.getItem() == Items.COMPASS;

        //CompassItem.isLodestoneCompass(heldStack)
        //NOTE: We are working not with entire stack, but with a single item ALWAYS. We cannot store more than 1 item in lodestone tracker
        //If there is a compass in block and we are empty-handed -> Get compass into players hand and remove it from block. Update block, play sound

        //If was NOT empty-handed and no compass in block -> Place compass into block, remove it from hand, update block, play sound
        System.out.println("Lodestone used:" + isCompass);
        return InteractionResult.SUCCESS;
    }
}
