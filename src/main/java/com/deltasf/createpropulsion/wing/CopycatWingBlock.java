package com.deltasf.createpropulsion.wing;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.Wing;
import org.valkyrienskies.mod.common.block.WingBlock;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.math.VoxelShaper;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CopycatWingBlock extends CopycatBlock implements WingBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final int width;
    private final Supplier<Item> baseItemSupplier;

    private static final List<BlockEntry<?>> entires = List.of(PropulsionBlocks.COPYCAT_WING, PropulsionBlocks.COPYCAT_WING_8, PropulsionBlocks.COPYCAT_WING_12, PropulsionBlocks.WING_BLOCK, PropulsionBlocks.TEMPERED_WING_BLOCK);
    private static final int placementHelperId = PlacementHelpers.register(new WingPlacementHelper(entires));

    private static final Map<Integer, VoxelShaper> wingShapers = Map.of(
        4, PropulsionShapes.WING,
        8, PropulsionShapes.WING_8,
        12, PropulsionShapes.WING_12
    );

    public CopycatWingBlock(Properties properties, int width, Supplier<Item> baseItemSupplier) {
        super(properties);
        this.width = width;
        this.baseItemSupplier = baseItemSupplier;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    public int getWidth() {
        return width;
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return wingShapers.get(this.width).get(state.getValue(FACING));
    }

    @Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        ItemStack heldItem = player.getItemInHand(hand);
        
        //Placement helper
        if (!player.isShiftKeyDown() && player.mayBuild()) {
			IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
            if (placementHelper.matchesItem(heldItem)) {
                placementHelper.getOffset(player, world, state, pos, ray)
					.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
				return InteractionResult.SUCCESS;
            }
        }

        return super.use(state, world, pos, player, hand, ray);
    }
    
    @Override
    public Wing getWing(@Nullable Level level, @Nullable BlockPos pos, @NotNull BlockState state) {
        Vec3i normal = state.getValue(FACING).getNormal();
        CopycatWingProperties properties = CopycatWingProperties.PROPERTIES_BY_WIDTH.getOrDefault(this.width, new CopycatWingProperties(0, 0));

        return new Wing(
            VectorConversionsMCKt.toJOMLD(MathUtility.AbsComponents(normal)),
            PropulsionConfig.BASE_WING_LIFT.get() + properties.lift(),
            PropulsionConfig.BASE_WING_DRAG.get() + properties.drag(),
            null,
            0
        );
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        BlockState material = getMaterial(level, pos);
        if (AllBlocks.COPYCAT_BASE.has(material) || (player != null && player.isShiftKeyDown())) {
            return new ItemStack(baseItemSupplier.get());
        }
        
        return material.getCloneItemStack(target, level, pos, player);
    }

    @Override
    public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos, BlockState state) {
        BlockState toState = reader.getBlockState(toPos);
        if (!toState.is(this)) {
            return false;
        }

        return state.getValue(FACING) == toState.getValue(FACING);
    }

    @Override
    public boolean isIgnoredConnectivitySide(BlockAndTintGetter reader, BlockState state, Direction face, @Nullable BlockPos fromPos, @Nullable BlockPos toPos) {
        if (fromPos == null || toPos == null) return true;

        BlockState toState = reader.getBlockState(toPos);
        Direction facing = state.getValue(FACING);

        if (!toState.is(this)) return facing != face.getOpposite();

        Direction toFacing = toState.getValue(FACING);
        BlockPos diff = toPos.subtract(fromPos);

        //Avoiding over-gap connections
        if (diff.getX() == 0 && diff.getZ() == 0 && diff.getY() != 0 && facing.getAxis() == Direction.Axis.Y && toFacing.getAxis() == Direction.Axis.Y) {
            return true;
        }
        if (diff.getY() == 0 && diff.getZ() == 0 && diff.getX() != 0 && facing.getAxis() == Direction.Axis.X && toFacing.getAxis() == Direction.Axis.X) {
            return true;
        }
        if (diff.getX() == 0 && diff.getY() == 0 && diff.getZ() != 0 && facing.getAxis() == Direction.Axis.Z && toFacing.getAxis() == Direction.Axis.Z) {
            return true;
        }

        return false;
    }

    @Override
    public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        int dropCount = this.width / 4;
        if (dropCount < 1) {
            return Collections.emptyList();
        }
        return List.of(new ItemStack(PropulsionBlocks.COPYCAT_WING.get(), dropCount));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (newState.getBlock() instanceof CopycatWingBlock) {
            if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
                level.removeBlockEntity(pos);
            }
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}