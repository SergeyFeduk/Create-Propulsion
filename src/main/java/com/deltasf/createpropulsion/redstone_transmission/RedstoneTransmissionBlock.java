package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

public class RedstoneTransmissionBlock extends AbstractEncasedShaftBlock implements IBE<RedstoneTransmissionBlockEntity> {
    public static final Property<Direction> HORIZONTAL_FACING = HorizontalKineticBlock.HORIZONTAL_FACING;

    public RedstoneTransmissionBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    public Class<RedstoneTransmissionBlockEntity> getBlockEntityClass() {
        return RedstoneTransmissionBlockEntity.class;
    }

    @Override
    public BlockEntityType<RedstoneTransmissionBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.REDSTONE_TRANSMISSION_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneTransmissionBlockEntity(PropulsionBlockEntities.REDSTONE_TRANSMISSION_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        BlockState state = super.getRotatedBlockState(originalState, targetedFace);
        if(state.getValue(AXIS).isHorizontal()) {
            return state.setValue(AXIS, state.getValue(HORIZONTAL_FACING).getAxis());
        }
        return state;
    }

    @Override
    public @NotNull BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis preferredAxis = RotatedPillarKineticBlock.getPreferredAxis(context);
        Direction.Axis axis;

       Player player = context.getPlayer();
        if (preferredAxis != null && (player == null || !player.isShiftKeyDown())) {
            axis = preferredAxis;
        } else {
            //Non shaft-aware reversed
            if (context.getNearestLookingDirection().getAxis().isVertical()) {
                axis = context.getHorizontalDirection().getAxis();
            } else {
                axis = Direction.Axis.Y;
            }
        }

        BlockState state = this.defaultBlockState().setValue(AXIS, axis);
        Direction facing = Direction.NORTH;
        
        if (axis.isVertical()) {
            facing = context.getHorizontalDirection().getOpposite();
        } else {
            for (Direction dir : context.getNearestLookingDirections()) {
                Direction candidate = dir.getOpposite();
                if (candidate.getAxis().isHorizontal() && candidate.getAxis() == axis) {
                    facing = candidate;
                    break;
                }
            }
        }

        return state.setValue(HORIZONTAL_FACING, facing);
    }

    @Override
    public boolean hasAnalogOutputSignal(@Nonnull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RedstoneTransmissionBlockEntity rtbe) {
            return rtbe.getComparatorOutput();
        }
        return 0;
    }
}
