package com.deltasf.createpropulsion.balloons.blocks.hot_air_burner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.balloons.blocks.AbstractHotAirInjectorBlock;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;

public class HotAirBurnerBlock extends AbstractHotAirInjectorBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public HotAirBurnerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(LIT, false)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
        builder.add(FACING);
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (player.isSpectator())
            return InteractionResult.PASS;

        if (!(level.getBlockEntity(pos) instanceof HotAirBurnerBlockEntity burnerEntity))
            return InteractionResult.PASS;

        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        ItemStack heldItem = player.getItemInHand(hand);
        HotAirBurnerFuelBehaviour fuelBehaviour = burnerEntity.getBehaviour(HotAirBurnerFuelBehaviour.TYPE);
        boolean hasFuel = !fuelBehaviour.fuelStack.isEmpty();
        boolean isHoldingBurnable = ForgeHooks.getBurnTime(heldItem, RecipeType.SMELTING) > 0;

        Direction facing = state.getValue(FACING);
        Vec3 frontNormal = Vec3.atLowerCornerOf(facing.getNormal());
        double dot = player.getLookAngle().dot(frontNormal);

        boolean isTargetingFront = dot < 0;

        if ((isHoldingBurnable || hasFuel) && isTargetingFront) {
            if (fuelBehaviour.handlePlayerInteraction(player, hand)) {
                return InteractionResult.SUCCESS;
            }
        } else {
            burnerEntity.cycleLever(player.isShiftKeyDown());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction baseDirection = context.getHorizontalDirection();
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
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HotAirBurnerBlockEntity burnerEntity) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), burnerEntity.getFuelStack());
            }
        }

        super.onRemove(state, level, pos, newState, isMoving); //This calls onBlockBroken from parent
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return PropulsionShapes.HOT_AIR_BURNER.get(Direction.NORTH);
        }
        Direction direction = pState.getValue(FACING);
        return PropulsionShapes.HOT_AIR_BURNER.get(direction);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pPos, @Nonnull BlockState pState) {
        return new HotAirBurnerBlockEntity(PropulsionBlockEntities.HOT_AIR_BURNER_BLOCK_ENTITY.get(), pPos, pState);
    }
}
