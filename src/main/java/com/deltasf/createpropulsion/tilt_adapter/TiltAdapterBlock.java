package com.deltasf.createpropulsion.tilt_adapter;

import javax.annotation.Nullable;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TiltAdapterBlock extends AbstractEncasedShaftBlock implements IBE<TiltAdapterBlockEntity> {
    public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");
    public static final BooleanProperty ALIGNED_X = BooleanProperty.create("aligned_x");

    public TiltAdapterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(AXIS, Direction.Axis.Y)
            .setValue(POSITIVE, true)
            .setValue(ALIGNED_X, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Player player = context.getPlayer();
        Direction placeDirection;

        if (player != null && !player.isShiftKeyDown()) {
            placeDirection = baseDirection.getOpposite();
        } else {
            placeDirection = baseDirection;
        }

        Direction.Axis axis = placeDirection.getAxis();
        boolean isPositive = placeDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE;

        boolean alignedX = false;
        if (axis == Direction.Axis.Y) {
            Direction horizontalLook = context.getHorizontalDirection();
            if (horizontalLook.getAxis() == Direction.Axis.X) {
                alignedX = true;
            } else {
                alignedX = false;
            }
        }

        return defaultBlockState()
            .setValue(AXIS, axis)
            .setValue(POSITIVE, isPositive)
            .setValue(ALIGNED_X, alignedX);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        Direction direction = getDirection(pState);
        if (direction.getAxis() == Axis.Y) direction = direction.getOpposite();
        return PropulsionShapes.TILT_ADAPTER.get(direction); 
    }

    public static Direction getDirection(BlockState state) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean isPositive = state.getValue(POSITIVE);
        
        switch (axis) {
            case X:
                return isPositive ? Direction.EAST : Direction.WEST;
            case Y:
                return isPositive ? Direction.UP : Direction.DOWN;
            case Z:
                return isPositive ? Direction.SOUTH : Direction.NORTH;
            default:
                return Direction.UP;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AXIS, POSITIVE, ALIGNED_X);
    }

    @Override
    public Class<TiltAdapterBlockEntity> getBlockEntityClass() {
        return TiltAdapterBlockEntity.class;
    }

    @Override
    public BlockEntityType<TiltAdapterBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TiltAdapterBlockEntity(PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return false;
    }
}
