package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.mojang.logging.LogUtils;
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

import javax.annotation.Nonnull;

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
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
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
        Direction.Axis axis;
        if (context.getNearestLookingDirection().getAxis().isHorizontal()) {
            axis = Direction.Axis.Y;
        } else {
            axis = context.getHorizontalDirection().getAxis();
        }
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()).setValue(AXIS, axis);
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return false;
    }
}
