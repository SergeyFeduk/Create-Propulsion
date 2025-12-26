package com.deltasf.createpropulsion.reaction_wheel;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ReactionWheelBlock extends DirectionalKineticBlock implements IBE<ReactionWheelBlockEntity> {
    public ReactionWheelBlock(Properties properties) {
        super(properties);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = !player.isShiftKeyDown() ? baseDirection.getOpposite() : baseDirection;
        } else {
            placeDirection = baseDirection;
        }

        return this.defaultBlockState().setValue(FACING, placeDirection);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity())) {
            if (!level.isClientSide) {
                ReactionWheelAttachment ship = ReactionWheelAttachment.get(level, pos);
                if (ship != null) {
                    ship.removeApplier((ServerLevel) level, pos);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<ReactionWheelBlockEntity> getBlockEntityClass() {
        return ReactionWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReactionWheelBlockEntity> getBlockEntityType() {
        return null;
        //return PropulsionBlockEntities.REACTION_WHEEL_BLOCK_ENTITY.get();
    }
}