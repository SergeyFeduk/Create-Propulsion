package com.deltasf.createpropulsion.optical_sensors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.registries.PropulsionItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public abstract class AbstractOpticalSensorBlock extends DirectionalBlock implements EntityBlock, IWrenchable, SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("redstone_power", 0, 15);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected AbstractOpticalSensorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(POWER);
        builder.add(POWERED);
        builder.add(WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public FluidState getFluidState(@Nonnull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return originalState;
    }

    @Override
    public boolean isSignalSource(@Nonnull BlockState state) {
        return state.getValue(POWER) > 0;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return new SmartBlockEntityTicker<>();
        } else {
            return new SmartBlockEntityTicker<>();
        }
    }

    protected abstract boolean isInteractionFace(BlockState state, BlockHitResult hit);

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        //TODO: Add sounds
        //Lenses actions are applicable to the front face only
        if (!isInteractionFace(state, hit)) {
            return super.use(state, level, pos, player, hand, hit);
        }
        //Safety check + get sensorBE
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AbstractOpticalSensorBlockEntity sensorBE)) {
            return super.use(state, level, pos, player, hand, hit);
        }
        ItemStack heldStack = player.getItemInHand(hand);
        //Handle client
        if (level.isClientSide) {
            if (heldStack.is(PropulsionItems.OPTICAL_LENS_TAG) || heldStack.isEmpty()) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        //Handle server
        if (heldStack.is(PropulsionItems.OPTICAL_LENS_TAG)) {
            if (sensorBE.insertLens(heldStack)) {
                return InteractionResult.CONSUME;
            } else {
                return InteractionResult.FAIL;
            }
        } else if (heldStack.isEmpty()) {
            ItemStack extractedLens = sensorBE.extractLastLens();
            if (!extractedLens.isEmpty()) {
                player.getInventory().placeItemBackInInventory(extractedLens);
                return InteractionResult.SUCCESS;
            }
        } else {
            //Not lens and not empty hand
            return InteractionResult.PASS;
        }
        //Should not happen
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    // Abstract methods
    @Override
    public abstract BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state);

    @Override
    public abstract BlockState getStateForPlacement(@Nonnull BlockPlaceContext context);

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractOpticalSensorBlockEntity sensorBlockEntity) {
                for (ItemStack lens : sensorBlockEntity.getLenses()) {
                    if (!lens.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lens);
                    }
                }
            }
            level.removeBlockEntity(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    protected abstract VoxelShaper getShapeMap();

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return getShapeMap().get(Direction.NORTH);
        }
        Direction direction = pState.getValue(FACING);
        if (direction == Direction.UP || direction == Direction.DOWN) direction = direction.getOpposite();
        return getShapeMap().get(direction);
    }

    @Override
    public abstract int getSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side);

    @Override
    public abstract int getDirectSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side);

    @Override
	public abstract boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side);
}
