package com.deltasf.createpropulsion.impact_sensor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ImpactSensorBlock extends Block implements EntityBlock {
    public ImpactSensorBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                ImpactSensorAttachment attachment = ImpactSensorAttachment.get(serverLevel, pos);
                if (attachment != null) {
                    attachment.removeSensor(pos);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return null;
        //return new ImpactSensorBlockEntity(PropulsionBlockEntities.IMPACT_SENSOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        /*if (type == PropulsionBlockEntities.IMPACT_SENSOR_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, be) -> {
                if (be instanceof ImpactSensorBlockEntity sensorBe) {
                    sensorBe.tick();
                }
            };
        }*/
        return null;
    }
}
