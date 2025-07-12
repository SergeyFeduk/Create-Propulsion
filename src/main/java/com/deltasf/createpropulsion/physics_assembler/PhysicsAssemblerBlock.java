package com.deltasf.createpropulsion.physics_assembler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class PhysicsAssemblerBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public PhysicsAssemblerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new PhysicsAssemblerBlockEntity(PropulsionBlockEntities.PHYSICAL_ASSEMBLER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        return PropulsionShapes.PHYSICS_ASSEMBLER.get(Direction.NORTH);
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || hit.getDirection() != Direction.UP) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof PhysicsAssemblerBlockEntity assembler)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        boolean assemblerHasGauge = assembler.hasGauge();

        if (heldItem.getItem() instanceof AssemblyGaugeItem && !assemblerHasGauge) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (assembler.canInsertGauge(heldItem)) {
                assembler.insertGauge(player, hand);
            }
            return InteractionResult.SUCCESS;

        } else if (heldItem.isEmpty() && assemblerHasGauge) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            ItemStack removedGauge = assembler.removeGauge();
            player.getInventory().placeItemBackInInventory(removedGauge);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof PhysicsAssemblerBlockEntity assemblerEntity) {
                assemblerEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    ItemStack stackInSlot = handler.getStackInSlot(0);
                    if (!stackInSlot.isEmpty()) {
                        NonNullList<ItemStack> itemsToDrop = NonNullList.withSize(1, stackInSlot);
                        Containers.dropContents(level, pos, itemsToDrop);
                    }
                });
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    //Handle redstone signal
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide()) return;
        doRedstoneCheck(level, state, pos);
    }

    private void doRedstoneCheck(Level level, BlockState state, BlockPos pos){
        //Get redstone powers
        boolean oldPowered = state.getValue(POWERED);
        boolean newPowered = level.getBestNeighborSignal(pos) > 0;
        if (oldPowered == newPowered) return; 
        //Update state
        BlockState newState = state.setValue(POWERED, newPowered);
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        if (!newPowered) return;
        
        //Invoke shipify
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PhysicsAssemblerBlockEntity physicsAssemblerBlockEntity) {
            physicsAssemblerBlockEntity.shipify();
        }
    }
}
