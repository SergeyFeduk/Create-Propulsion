package com.deltasf.createpropulsion.propeller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.physics_assembler.AssemblyUtility;
import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PropellerBlock extends DirectionalKineticBlock implements IBE<PropellerBlockEntity> {
    public static final float INTERACTION_RPM_THRESHOLD = 20.0f;
    private static final int ERROR_MESSAGE_COLOR = 0xFF_ff5d6c;

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

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        boolean isSneaking = player != null && player.isShiftKeyDown();

        //Find best direction based on surrounding rotational blocks
        Direction preferredDirection = getPreferredDirection(context);
        if (preferredDirection != null && !isSneaking) {
            return this.defaultBlockState().setValue(FACING, preferredDirection);
        }
        
        //Fallback
        Direction baseDirection = context.getNearestLookingDirection();
        return this.defaultBlockState().setValue(FACING, baseDirection.getOpposite());
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PropellerBlockEntity propellerBE))
            return InteractionResult.PASS;

        //RPM check
        if (propellerBE.getBladeCount() != 1) {
            float rpm = Math.abs(level.isClientSide ? propellerBE.visualRPM : propellerBE.getInternalRPM());

            if (rpm > INTERACTION_RPM_THRESHOLD) {
                if (level.isClientSide && player instanceof LocalPlayer localPlayer)  {
                    localPlayer.displayClientMessage(
                        Component.translatable("createpropulsion.propeller.must_be_stopped")
                        .withStyle(s -> s.withColor(ERROR_MESSAGE_COLOR)), true); 
                    return InteractionResult.SUCCESS; //While this is for fail case - only SUCCESS causes arm swing animation
                }
                return InteractionResult.FAIL;
            }
        }

        //Blade insertion
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof PropellerBladeItem) {

            propellerBE.getSpatialHandler().triggerImmediateScan();

            if (!propellerBE.getSpatialHandler().getObstructedBlocks().isEmpty()) {
                if (level.isClientSide) {
                    showBounds(pos, state, player);
                    return InteractionResult.SUCCESS; //While this is for fail case - only SUCCESS causes arm swing animation
                }
                return InteractionResult.FAIL;
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;

            Vec3 localHit = hit.getLocation().subtract(Vec3.atCenterOf(pos));
            if (propellerBE.addBlade(heldItem, localHit)) {
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0f, 1.2f);
                return InteractionResult.SUCCESS;
            }

        } else if (heldItem.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (level.isClientSide) return InteractionResult.SUCCESS;

            if (player.isShiftKeyDown()) {
                if (propellerBE.getBladeCount() > 0) {
                    propellerBE.flipBladeDirection();
                    AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() * .5f + .5f);
                    return InteractionResult.SUCCESS;
                }
            } else {
                ItemStack removedBlade = propellerBE.removeBlade();
                if (!removedBlade.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(removedBlade);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity())) {
            if (!level.isClientSide) {
                //Use the new PropellerForceAttachment
                PropellerAttachment ship = PropellerAttachment.get(level, pos);
                if (ship != null) {
                    ship.removeApplier((ServerLevel) level, pos);
                }
                //Drop blades
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof PropellerBlockEntity propellerBlockEntity) {
                    for (int i = 0; i < propellerBlockEntity.bladeInventory.getSlots(); i++) {
                        ItemStack stackInSlot = propellerBlockEntity.bladeInventory.getStackInSlot(i);
                        if (!stackInSlot.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stackInSlot);
                        }
                    }
                }

            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide()) return;
        
        withBlockEntityDo(level, pos, (be) -> be.getSpatialHandler().triggerImmediateScan());
    }

    public static Direction getPreferredDirection(BlockPlaceContext context) {
        Direction preferredDirection = null;
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = context.getClickedPos().relative(side);
            BlockState neighborState = context.getLevel().getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof IRotate) {
                IRotate neighborRotate = (IRotate) neighborState.getBlock();
                if (neighborRotate.hasShaftTowards(context.getLevel(), neighborPos, neighborState, side.getOpposite())) {
                    Direction facingDirection = side.getOpposite();
                    if (preferredDirection != null && preferredDirection != facingDirection) {
                        return null; //Fallback
                    }
                    preferredDirection = facingDirection;
                }
            }
        }
        return preferredDirection;
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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == getBlockEntityType()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }

    @Override
    public Class<PropellerBlockEntity> getBlockEntityClass() {
        return PropellerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PropellerBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.PROPELLER_BLOCK_ENTITY.get();
    }

    private void showBounds(BlockPos pos, BlockState state, Player player) {
        if (!(player instanceof LocalPlayer localPlayer)) return;

        if (!GogglesItem.isWearingGoggles(localPlayer)) {
            Vec3 contract = Vec3.atLowerCornerOf(state.getValue(DirectionalKineticBlock.FACING).getNormal());
            Outliner.getInstance().showAABB(Pair.of("propeller", pos), 
                new AABB(pos).inflate(1)
                .deflate(contract.x, contract.y, contract.z))
                .colored(AssemblyUtility.CANCEL_COLOR);
        }

        localPlayer.displayClientMessage(
            Component.translatable("createpropulsion.propeller.not_enough_space")
            .withStyle(s -> s.withColor(ERROR_MESSAGE_COLOR)), true); 
    }
}
