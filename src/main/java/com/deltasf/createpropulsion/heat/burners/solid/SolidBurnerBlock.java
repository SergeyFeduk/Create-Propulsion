package com.deltasf.createpropulsion.heat.burners.solid;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SolidBurnerBlock extends AbstractBurnerBlock {
    public SolidBurnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new SolidBurnerBlockEntity(PropulsionBlockEntities.SOLID_BURNER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.SOLID_BURNER_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}
