package com.deltasf.createpropulsion.heat.engine;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class StirlingEngineBlock extends DirectionalKineticBlock implements IBE<StirlingEngineBlockEntity> {
    
    public StirlingEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(super.defaultBlockState());
    }

    //TODO: Update similarly to propeller
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    @Override
    public Class<StirlingEngineBlockEntity> getBlockEntityClass() {
        return StirlingEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<StirlingEngineBlockEntity> getBlockEntityType() {
        return null;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(FACING)
                .getAxis() == face.getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.getValue(FACING)
                .getAxis();
    }
}
