package com.deltasf.createpropulsion.thruster;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.ship.ForceInducedShip;
import com.deltasf.createpropulsion.utility.ShapeBuilder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

@SuppressWarnings("deprecation")
public class ThrusterBlock extends DirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("redstone_power", 0, 15);
    public static final VoxelShaper BLOCK_SHAPE;

    static {
        VoxelShape shape = Shapes.empty();

        shape = Shapes.join(shape, Block.box(2, 2, 0, 14, 14, 2), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(1, 1, 2, 15, 15, 14), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(3, 3, 14, 13, 13, 16), BooleanOp.OR);

        BLOCK_SHAPE = ShapeBuilder.shapeBuilder(shape).forDirectional(Direction.NORTH);
    }

    public ThrusterBlock(Properties properties){
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWER);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ThrusterBlockEntity(null, pos, state);
    }

    //Add/remove thruster force applier to the ship
    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) {
            ForceInducedShip ship = ForceInducedShip.get(level, pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ThrusterBlockEntity thrusterBlockEntity) {
                if (ship != null) {
                    //Create thruster data
                    ThrusterData data = thrusterBlockEntity.getThrusterData();
                    data.setDirection(VectorConversionsMCKt.toJOMLD(state.getValue(FACING).getNormal()));
                    data.setThrust(0);
                    //Create and add applier
                    ThrusterForceApplier applier = new ThrusterForceApplier(data);
                    ship.addApplier(pos, applier);
                }
                //Invoke initial redstone and obstruction check
                doRedstoneCheck(level, state, pos); //This does not call obstruction due to skipping logic
                calculateObstruction(thrusterBlockEntity, level, pos, state);
            }
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!level.isClientSide()) {
            ForceInducedShip ship = ForceInducedShip.get(level, pos);
            if (ship != null) {
                ship.removeApplier((ServerLevel)level, pos);
            }
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity != null)
            ((SmartBlockEntity)entity).destroy(); //Probably required
        super.onRemove(state, level, pos, newState, isMoving);
    }
    //Handle redstone level
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide()) return;
        doRedstoneCheck(level, state, pos);
    }
    private void doRedstoneCheck(Level level, BlockState state, BlockPos pos){
        //Get redstone powers
        int newRedstonePower = level.getBestNeighborSignal(pos);
        int oldRedstonePower = state.getValue(POWER);
        if (newRedstonePower == oldRedstonePower) return;
        //Update state
        BlockState newState = state.setValue(POWER, newRedstonePower);
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        //Calculate obstruction
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ThrusterBlockEntity thrusterBlockEntity) {
            calculateObstruction(thrusterBlockEntity, level, pos, state);
        }
    }

    private void calculateObstruction(ThrusterBlockEntity thrusterBlockEntity, Level level, BlockPos pos, BlockState state){
        thrusterBlockEntity.calculateObstruction(level, pos, state.getValue(FACING));
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return (cleintLevel, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ThrusterBlockEntity thrusterBlockEntity) {
                    thrusterBlockEntity.clientTick(cleintLevel, pos, state1, thrusterBlockEntity);
                }
            };
        } else {
            if (type == CreatePropulsion.THRUSTER_BLOCK_ENTITY.get()) {
                return new SmartBlockEntityTicker<>();
            }
        }
        return null;
    }
}
