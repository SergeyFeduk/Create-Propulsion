package com.deltasf.createpropulsion.impact_sensor;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ImpactSensorBlock extends Block implements EntityBlock {
    public ImpactSensorBlock(Properties properties) {
        super(properties);
    }

    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ImpactSensorBlockEntity(null, pos, state);
    }
}
