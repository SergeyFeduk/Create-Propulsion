package com.deltasf.createpropulsion.balloons.injectors;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractHotAirInjectorBlock extends Block implements EntityBlock {
    public AbstractHotAirInjectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public abstract BlockEntity newBlockEntity(@Nonnull BlockPos pPos, @Nonnull BlockState pState);

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        //Final destination
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractHotAirInjectorBlockEntity haiBlockEntity) {
                haiBlockEntity.onBlockBroken();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public void triggerScan(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractHotAirInjectorBlockEntity haiBlockEntity) {
            haiBlockEntity.scan();
        }
    }
}
