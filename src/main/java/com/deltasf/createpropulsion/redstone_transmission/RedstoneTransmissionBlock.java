package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class RedstoneTransmissionBlock extends AbstractEncasedShaftBlock implements IBE<RedstoneTransmissionBlockEntity> {
    public static final int MAX_VALUE = 255;
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty SHIFT_LEVEL = IntegerProperty.create("shift_level", 0, 255);

    public RedstoneTransmissionBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(SHIFT_LEVEL, 128));
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
        builder.add(HORIZONTAL_FACING, SHIFT_LEVEL);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return originalState.setValue(HORIZONTAL_FACING, originalState.getValue(HORIZONTAL_FACING).getClockWise());
    }

    @Override
    public @NotNull BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return false;
    }
}
