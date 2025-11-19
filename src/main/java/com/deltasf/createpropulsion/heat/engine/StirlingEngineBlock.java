package com.deltasf.createpropulsion.heat.engine;

import javax.annotation.Nullable;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StirlingEngineBlock extends DirectionalKineticBlock implements IBE<StirlingEngineBlockEntity> {

    public StirlingEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(super.defaultBlockState());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean isSneaking = player != null && player.isShiftKeyDown();
        Direction preferred = null;

        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockState neighborState = level.getBlockState(pos.relative(side));
            if (neighborState.getBlock() instanceof IRotate neighborRotate) {
                if (neighborRotate.hasShaftTowards(level, pos.relative(side), neighborState, side.getOpposite())) {
                    if (preferred != null && preferred != side) {
                        preferred = null;
                        break;
                    }
                    preferred = side;
                }
            }
        }

        if (preferred != null && !isSneaking) {
            return defaultBlockState().setValue(FACING, preferred);
        }
        
        Direction placedFacing = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, placedFacing);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return PropulsionShapes.STIRLING_ENGINE.get(Direction.NORTH);
        }
        Direction direction = pState.getValue(FACING);
        return PropulsionShapes.STIRLING_ENGINE.get(direction);
    }

    @Override
    public Class<StirlingEngineBlockEntity> getBlockEntityClass() {
        return StirlingEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<StirlingEngineBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY.get();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == getBlockEntityType()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}