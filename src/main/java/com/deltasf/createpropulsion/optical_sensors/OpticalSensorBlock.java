package com.deltasf.createpropulsion.optical_sensors;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.PropulsionBlockEntities;
import com.deltasf.createpropulsion.PropulsionShapes;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class OpticalSensorBlock extends AbstractOpticalSensorBlock {
    public OpticalSensorBlock(Properties properties){
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new OpticalSensorBlockEntity(PropulsionBlockEntities.OPTICAL_SENSOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = player.isShiftKeyDown() ? baseDirection : baseDirection.getOpposite();
        } else {
            placeDirection = baseDirection.getOpposite();
        }
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        boolean isWaterlogged = fluidstate.getType() == Fluids.WATER;

        return this.defaultBlockState()
                   .setValue(FACING, placeDirection)
                   .setValue(WATERLOGGED, isWaterlogged);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.relative(facing.getOpposite()), state.getBlock());
        }
    }

    @Override
    protected VoxelShaper getShapeMap() { return PropulsionShapes.OPTICAL_SENSOR; }

    //Redstone

    @Override
    public int getSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side){
        return blockState.getValue(FACING).getOpposite() == side ? 0 : blockState.getValue(POWER);
    }

    @Override
    public int getDirectSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side) {
        return blockState.getValue(FACING) == side ? blockState.getValue(POWER) : 0;
    }

    @Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return side != state.getValue(FACING).getOpposite();
	}
}
