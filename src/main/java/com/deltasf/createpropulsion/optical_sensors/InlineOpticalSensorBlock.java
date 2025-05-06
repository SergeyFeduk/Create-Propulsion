package com.deltasf.createpropulsion.optical_sensors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.utility.ShapeBuilder;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class InlineOpticalSensorBlock extends DirectionalBlock implements EntityBlock, IWrenchable, SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = IntegerProperty.create("redstone_power", 0, 15);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final VoxelShaper BLOCK_SHAPE;

    //CBC placement on projectiles compat
    public static final TagKey<Item> CBC_PROJECTILE_ITEM_TAG =
        TagKey.create(Registries.ITEM, new ResourceLocation("createbigcannons", "big_cannon_projectiles"));
    private static Set<Block> validCbcSupportBlocks = null;
    private static final Object initLock = new Object();

    static {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Block.box(4, 4, 10, 12, 12, 16), BooleanOp.OR);
        BLOCK_SHAPE = ShapeBuilder.shapeBuilder(shape).forDirectional(Direction.NORTH);
    }

    public InlineOpticalSensorBlock(Properties properties){
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
        return new InlineOpticalSensorBlockEntity(null, pos, state);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        boolean isWaterlogged = fluidstate.getType() == Fluids.WATER;

        return this.defaultBlockState()
                   .setValue(FACING, context.getClickedFace())
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
            level.updateNeighborsAt(pos.relative(state.getValue(InlineOpticalSensorBlock.FACING).getOpposite()), state.getBlock());
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

    @Override
	public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide)
			return;
        
        Direction blockFacing = state.getValue(FACING);
		if (fromPos.equals(pos.relative(blockFacing.getOpposite()))) {
			if (!canSurvive(state, level, pos)) {
				level.destroyBlock(pos, true);
				return;
			}
		}
    }

    @Override
	public boolean isPathfindable(@Nonnull BlockState state, @Nonnull BlockGetter reader, @Nonnull BlockPos pos, @Nonnull PathComputationType type) {
		return false;
	}

    //Redstone signal emitter is strictly to the FACING side
    @Override
    public int getSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side){
        return blockState.getValue(FACING) == side ? blockState.getValue(POWER) : 0;
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
		return side == state.getValue(FACING);
	}

    @Override
	public boolean canSurvive(@Nonnull BlockState state, @Nonnull LevelReader level, @Nonnull BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        boolean faceIsSturdy = supportState.isFaceSturdy(level, supportPos, facing);
        if (faceIsSturdy) return true;
        //We can place optical sensors on projectiles too
        if (CreatePropulsion.CBC_ACTIVE) {
            Set<Block> projectileBlocks = getOrCreateProjectileBlocks();
            boolean isOnProjectile = projectileBlocks.contains(supportState.getBlock());
            //Can only be placed on elongated sides
            if (isOnProjectile) {
                Direction blockDirection = supportState.getValue(DirectionalBlock.FACING);
                return blockDirection == facing || blockDirection.getOpposite() == facing;
            }
        }
        return false;
	}

    //CBC compat projectile check
    private static Set<Block> getOrCreateProjectileBlocks() {
        // Double-checked locking pattern for thread-safe lazy initialization
        if (validCbcSupportBlocks == null) {
            synchronized (initLock) {
                if (validCbcSupportBlocks == null) {
                    if (!CreatePropulsion.CBC_ACTIVE) {
                        validCbcSupportBlocks = Collections.emptySet();
                    } else {
                        Set<Block> tempSet = new HashSet<>();
                        Optional<HolderSet.Named<Item>> tagOptional = BuiltInRegistries.ITEM.getTag(CBC_PROJECTILE_ITEM_TAG);
                        HolderSet.Named<Item> itemHolders = tagOptional.get();
                        for (Holder<Item> itemHolder : itemHolders) {
                            Item item = itemHolder.value();
                            Block block = Block.byItem(item);
                            if (block != null && block != Blocks.AIR) {
                                tempSet.add(block);
                            }
                        }
                        validCbcSupportBlocks = tempSet;
                    }
                }
            }
        }
        return validCbcSupportBlocks;
    }

    //Ticker
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return new SmartBlockEntityTicker<>();
    }

}
