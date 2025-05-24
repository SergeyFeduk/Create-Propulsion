package com.deltasf.createpropulsion.tilt_sensor;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TiltSensorBlock extends Block implements EntityBlock {
    
    public TiltSensorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new TiltSensorBlockEntity(PropulsionBlockEntities.LODESTONE_TRACKER_BLOCK_ENTITY.get(), pos, state);
    }
}
