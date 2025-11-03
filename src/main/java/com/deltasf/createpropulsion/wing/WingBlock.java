package com.deltasf.createpropulsion.wing;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Wing;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.deltasf.createpropulsion.utility.MathUtility;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WingBlock extends DirectionalBlock implements org.valkyrienskies.mod.common.block.WingBlock {

    private static final List<BlockEntry<?>> entires = List.of(PropulsionBlocks.COPYCAT_WING, PropulsionBlocks.COPYCAT_WING_8, PropulsionBlocks.COPYCAT_WING_12, PropulsionBlocks.WING_BLOCK);
    private static final int placementHelperId = PlacementHelpers.register(new WingPlacementHelper(entires));

    public WingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
	public InteractionResult use(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
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
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public Wing getWing(@Nullable Level level, @Nullable BlockPos pos, @NotNull BlockState state) {
        Vector3d normal = VectorConversionsMCKt.toJOMLD(MathUtility.AbsComponents(state.getValue(FACING).getNormal()));
        return new Wing(normal, PropulsionConfig.BASE_WING_LIFT.get(), PropulsionConfig.BASE_WING_DRAG.get(), null, 0);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return PropulsionShapes.WING.get(Direction.UP);
        }
        return PropulsionShapes.WING.get(pState.getValue(FACING));
    }
}
