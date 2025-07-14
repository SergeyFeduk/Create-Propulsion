package com.deltasf.createpropulsion.thruster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.registries.PropulsionShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public abstract class AbstractThrusterBlock extends DirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("redstone_power", 0, 15);

    protected AbstractThrusterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return PropulsionShapes.THRUSTER.get(Direction.NORTH);
        }
        Direction direction = pState.getValue(FACING);
        if (direction == Direction.UP || direction == Direction.DOWN) direction = direction.getOpposite();
        return PropulsionShapes.THRUSTER.get(direction);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = !player.isShiftKeyDown() ? baseDirection : baseDirection.getOpposite();
        } else {
            placeDirection = baseDirection.getOpposite();
        }

        return this.defaultBlockState().setValue(FACING, placeDirection);
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER);
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state);

    @Nullable
    @Override
    public abstract <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type);

    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide()) return;

        ThrusterForceAttachment ship = ThrusterForceAttachment.get(level, pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractThrusterBlockEntity thrusterBlockEntity) {
            if (ship != null) {
                // Initialize thruster data for Valkyrien Skies
                ThrusterData data = thrusterBlockEntity.getThrusterData();
                data.setDirection(VectorConversionsMCKt.toJOMLD(state.getValue(FACING).getNormal()));
                data.setThrust(0);
                ThrusterForceApplier applier = new ThrusterForceApplier(data);
                ship.addApplier(pos, applier);
            }
            // Trigger an initial check for redstone power and obstruction
            doRedstoneCheck(level, state, pos);
        }
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (level.isClientSide()) return;

        ThrusterForceAttachment ship = ThrusterForceAttachment.get(level, pos);
        if (ship != null) {
            ship.removeApplier((ServerLevel) level, pos);
        }
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide()) return;
        doRedstoneCheck(level, state, pos);
    }

    private void doRedstoneCheck(Level level, BlockState state, BlockPos pos) {
        int newRedstonePower = level.getBestNeighborSignal(pos);
        int oldRedstonePower = state.getValue(POWER);
        if (newRedstonePower == oldRedstonePower) return;

        BlockState newState = state.setValue(POWER, newRedstonePower);
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractThrusterBlockEntity thrusterBlockEntity) {
            thrusterBlockEntity.calculateObstruction(level, pos, state.getValue(FACING));
            thrusterBlockEntity.updateThrust(newState);
            thrusterBlockEntity.setChanged();
        }
    }

    @Override
    public BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
}
