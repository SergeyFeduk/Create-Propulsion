package com.deltasf.createpropulsion.tilt_adapter;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

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
