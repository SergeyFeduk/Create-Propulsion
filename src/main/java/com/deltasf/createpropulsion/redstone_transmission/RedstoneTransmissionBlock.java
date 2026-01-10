package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
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
        BlockState result = super.getStateForPlacement(context);
        Direction[] directions = context.getNearestLookingDirections();
        Direction facing = Direction.NORTH;
        Direction.Axis shaft = result.getValue(AXIS);
        for (int i = 0; i < directions.length; i++) {
            facing = directions[i].getOpposite();
            Direction.Axis faceAxis = facing.getAxis();
            if(faceAxis.isHorizontal() && (shaft.isVertical() || faceAxis.equals(shaft))) break;
        }
        return result.setValue(HORIZONTAL_FACING, facing);
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return false;
    }
}
