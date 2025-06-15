package com.deltasf.createpropulsion.optical_sensors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;

@SuppressWarnings("deprecation")
public class InlineOpticalSensorBlock extends AbstractOpticalSensorBlock {
    public static final TagKey<Item> CBC_PROJECTILE_ITEM_TAG =
        TagKey.create(Registries.ITEM, new ResourceLocation("createbigcannons", "big_cannon_projectiles"));
    private static Set<Block> validCbcSupportBlocks = null;
    private static final Object initLock = new Object();

    public InlineOpticalSensorBlock(Properties properties){
        super(properties);
    }


    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new InlineOpticalSensorBlockEntity(PropulsionBlockEntities.INLINE_OPTICAL_SENSOR_BLOCK_ENTITY.get(), pos, state);
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
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.relative(facing.getOpposite()), state.getBlock());
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected VoxelShaper getShapeMap() {
        return PropulsionShapes.INLINE_OPTICAL_SENSOR;
    }

    //Redstone

    @Override
    public int getSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side){
        int power = blockState.getValue(POWER);
        return blockState.getValue(FACING) == side ? power : 0;
    }

    @Override
    public int getDirectSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side) {
        int power = blockState.getValue(POWER);
        return blockState.getValue(FACING) == side ? power : 0;
    }

    @Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return side == state.getValue(FACING);
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

    @Override
	public boolean canSurvive(@Nonnull BlockState state, @Nonnull LevelReader level, @Nonnull BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);

        boolean faceIsSturdy = supportState.isFaceSturdy(level, supportPos, facing);
        if (faceIsSturdy) {
            return true;
        }

        if (PropulsionCompatibility.CBC_ACTIVE) {
            Set<Block> projectileBlocks = getOrCreateProjectileBlocks();
            boolean isOnProjectile = projectileBlocks.contains(supportState.getBlock());

            if (isOnProjectile) {
                if (supportState.hasProperty(DirectionalBlock.FACING)) {
                    Direction projectileDirection = supportState.getValue(DirectionalBlock.FACING);
                    return projectileDirection == facing || projectileDirection.getOpposite() == facing;
                }
            }
        }

        return false;
	}

    private static Set<Block> getOrCreateProjectileBlocks() {
        if (validCbcSupportBlocks == null) {
            synchronized (initLock) {
                if (validCbcSupportBlocks == null) {
                    if (!PropulsionCompatibility.CBC_ACTIVE) {
                        validCbcSupportBlocks = Collections.emptySet();
                    } else {
                        Set<Block> tempSet = new HashSet<>();
                        Optional<HolderSet.Named<Item>> tagOptional = BuiltInRegistries.ITEM.getTag(CBC_PROJECTILE_ITEM_TAG);
                        if (tagOptional.isPresent()) {
                            HolderSet.Named<Item> itemHolders = tagOptional.get();
                            for (Holder<Item> itemHolder : itemHolders) {
                                Item item = itemHolder.value();
                                Block block = Block.byItem(item);
                                if (block != Blocks.AIR) {
                                    tempSet.add(block);
                                }
                            }
                        }
                        validCbcSupportBlocks = Collections.unmodifiableSet(tempSet); 
                    }
                }
            }
        }
        return validCbcSupportBlocks;
    }
}
