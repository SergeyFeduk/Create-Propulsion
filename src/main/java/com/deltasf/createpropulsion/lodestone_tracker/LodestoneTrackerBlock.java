package com.deltasf.createpropulsion.lodestone_tracker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class LodestoneTrackerBlock extends Block implements EntityBlock, IWrenchable {

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        return PropulsionShapes.LODESTONE_TRACKER.get(Direction.NORTH);
    }

    public LodestoneTrackerBlock(Properties properties){
        super(properties);
        this.registerDefaultState(this.stateDefinition.any());
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LodestoneTrackerBlockEntity(PropulsionBlockEntities.LODESTONE_TRACKER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LodestoneTrackerBlockEntity trackerBlockEntity) {
                ItemStack compass = trackerBlockEntity.getCompass();
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), compass);
            }
            level.removeBlockEntity(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //Handling hand interactions
    @SuppressWarnings("null")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        if (ray.getDirection() != Direction.UP) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LodestoneTrackerBlockEntity trackerBlockEntity)) return InteractionResult.PASS;

        ItemStack heldStack = player.getItemInHand(hand);
        boolean trackerHasCompass = trackerBlockEntity.hasCompass();
        boolean playerHoldingCompass = heldStack.getItem() == Items.COMPASS;

        if (playerHoldingCompass && !trackerHasCompass) {
            // Player has compass, tracker is empty -> Place compass
            ItemStack compassToPlace = heldStack.split(1);

            //Determine insertion direction
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
            Direction insertionDirection;

            if (ship != null) {
                Vec3 playerLookWorld = player.getLookAngle();
                Vector3d playerLookWorldJoml = new Vector3d(playerLookWorld.x, playerLookWorld.y, playerLookWorld.z);
                Vector3d playerLookShipJoml = ship.getWorldToShip().transformDirection(playerLookWorldJoml, new Vector3d());

                double relativeYawRadians = Math.atan2(-playerLookShipJoml.x, playerLookShipJoml.z);
                double relativeYawDegrees = Math.toDegrees(relativeYawRadians);

                insertionDirection = Direction.fromYRot(relativeYawDegrees);
            } else {
                insertionDirection = player.getDirection();
            }

            //Handle compass insertion
            trackerBlockEntity.setCompass(compassToPlace, insertionDirection);
            AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
            dirtyBlockEntity(level, pos);
            return InteractionResult.CONSUME;

        } else if (heldStack.isEmpty() && trackerHasCompass) {
            // Player has empty hand, tracker has compass -> Take compass
            ItemStack takenCompass = trackerBlockEntity.removeCompass();
            player.getInventory().placeItemBackInInventory(takenCompass);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
            dirtyBlockEntity(level, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    private void dirtyBlockEntity(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof LodestoneTrackerBlockEntity trackerBlockEntity) {
            trackerBlockEntity.isOutputDirty = true;
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return new SmartBlockEntityTicker<>();
    }

    //Wrench behaviour

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof LodestoneTrackerBlockEntity trackerBlockEntity) {
            trackerBlockEntity.toggleInverted();
            if (trackerBlockEntity.IsInverted()) {
                level.playSound(null, context.getClickedPos(), SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.7f, 0.8f);
            } else {
                IWrenchable.playRotateSound(level, context.getClickedPos());
            }
        }
        
        return InteractionResult.SUCCESS;
    }

    //Redstone
    @Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return side != Direction.DOWN && side != Direction.UP;
	}

    @Override
    public int getSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side){
        BlockEntity be = blockAccess.getBlockEntity(pos);
        if (be instanceof LodestoneTrackerBlockEntity trackerBlockEntity) {
            boolean invertedDirection = trackerBlockEntity.IsInverted();
            if (invertedDirection) {
                if (side == Direction.NORTH) return trackerBlockEntity.powerNorth();
                if (side == Direction.EAST) return trackerBlockEntity.powerEast();
                if (side == Direction.SOUTH) return trackerBlockEntity.powerSouth();
                if (side == Direction.WEST) return trackerBlockEntity.powerWest();
            } else {
                if (side == Direction.NORTH) return trackerBlockEntity.powerSouth();
                if (side == Direction.EAST) return trackerBlockEntity.powerWest();
                if (side == Direction.SOUTH) return trackerBlockEntity.powerNorth();
                if (side == Direction.WEST) return trackerBlockEntity.powerEast();
            }
        }
        return 0;
    }

    @Override
    public boolean isSignalSource(@Nonnull BlockState state){
        return true;
    }
}
