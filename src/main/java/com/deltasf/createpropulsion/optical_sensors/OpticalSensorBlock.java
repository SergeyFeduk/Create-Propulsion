package com.deltasf.createpropulsion.optical_sensors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.utility.ShapeBuilder;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;

@SuppressWarnings("deprecation")
public class OpticalSensorBlock extends DirectionalBlock implements EntityBlock, IWrenchable, SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("redstone_power", 0, 15);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final VoxelShaper BLOCK_SHAPE;

    static {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Block.box(0, 0, 4, 16, 16, 16), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(4, 4, 0, 12, 12, 4), BooleanOp.OR);
        BLOCK_SHAPE = ShapeBuilder.shapeBuilder(shape).forDirectional(Direction.NORTH);
    }

    public OpticalSensorBlock(Properties properties){
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false)
            .setValue(WATERLOGGED, false));
    }

    @Override
    public FluidState getFluidState(@Nonnull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new OpticalSensorBlockEntity(null, pos, state);
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
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return originalState;
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {   
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.relative(state.getValue(OpticalSensorBlock.FACING).getOpposite()), state.getBlock());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return BLOCK_SHAPE.get(Direction.NORTH);
        }
        Direction direction = pState.getValue(FACING);
        if (direction == Direction.UP || direction == Direction.DOWN) direction = direction.getOpposite(); //Because WTF
        return BLOCK_SHAPE.get(direction);
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(POWER);
        builder.add(POWERED);
        builder.add(WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    //Redstone signal emitter is strictly to the FACING side
    @Override
    public int getSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side){
        return blockState.getValue(FACING).getOpposite() == side ? 0 : blockState.getValue(POWER);
    }

    @Override
    public int getDirectSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side) {
        return blockState.getValue(FACING) == side ? blockState.getValue(POWER) : 0;
    }

    @Override
    public boolean isSignalSource(@Nonnull BlockState state){
        return state.getValue(POWER) > 0;
    }

    @Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return side != state.getValue(FACING).getOpposite();
	}

    //Ticker
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return new SmartBlockEntityTicker<>();
    }
}
