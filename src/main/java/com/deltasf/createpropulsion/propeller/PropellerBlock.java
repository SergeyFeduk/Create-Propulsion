package com.deltasf.createpropulsion.propeller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

//Let me try this IBE out
public class PropellerBlock extends DirectionalKineticBlock implements IBE<PropellerBlockEntity> {
    public PropellerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    //TODO: Have shaft-aware placement logic here
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = !player.isShiftKeyDown() ? baseDirection.getOpposite() : baseDirection;
        } else {
            placeDirection = baseDirection;
        }

        return this.defaultBlockState().setValue(FACING, placeDirection);
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PropellerBlockEntity propellerBE))
            return InteractionResult.PASS;


        if (propellerBE.getBladeCount() != 1) {
            if (Math.abs(propellerBE.getSpeed()) > 2) {
                // TODO: Notify player that propeller must be stopped
                return InteractionResult.FAIL;
            }
        }

        //TODO: Different sounds
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof PropellerBladeItem) {
            Vec3 localHit = hit.getLocation().subtract(Vec3.atCenterOf(pos));

            if (propellerBE.addBlade(heldItem, localHit)) {
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0f, 1.2f);
                return InteractionResult.SUCCESS;
            }
        } else if (heldItem.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            ItemStack removedBlade = propellerBE.removeBlade();
            if (!removedBlade.isEmpty()) {
                player.getInventory().placeItemBackInInventory(removedBlade);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity())) {
            if (!level.isClientSide) {
                // Use the new PropellerForceAttachment
                PropellerAttachment ship = PropellerAttachment.get(level, pos);
                if (ship != null) {
                    ship.removeApplier((ServerLevel) level, pos);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return PropulsionShapes.PROPELLER.get(Direction.NORTH);
        }
        Direction direction = pState.getValue(FACING);
        if (direction == Direction.UP || direction == Direction.DOWN) direction = direction.getOpposite();
        return PropulsionShapes.PROPELLER.get(direction);
    }

    @Override
    public Class<PropellerBlockEntity> getBlockEntityClass() {
        return PropellerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PropellerBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.PROPELLER_BLOCK_ENTITY.get();
    }
}
