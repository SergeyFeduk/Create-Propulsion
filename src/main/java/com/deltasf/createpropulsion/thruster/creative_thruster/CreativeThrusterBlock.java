package com.deltasf.createpropulsion.thruster.creative_thruster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.thruster.AbstractThrusterBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeThrusterBlock extends AbstractThrusterBlock {
    public CreativeThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return null;
        //return new CreativeThrusterBlockEntity(PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        /*if (type == PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }*/
        return null;
    }
}
