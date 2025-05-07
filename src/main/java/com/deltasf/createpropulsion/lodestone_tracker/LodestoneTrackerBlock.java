package com.deltasf.createpropulsion.lodestone_tracker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.utility.ShapeBuilder;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LodestoneTrackerBlock extends Block implements EntityBlock {
    public static final VoxelShaper BLOCK_SHAPE;
    public static final IntegerProperty POWER_NORTH = IntegerProperty.create("north_power", 0, 15);
    public static final IntegerProperty POWER_EAST = IntegerProperty.create("east_power", 0, 15);
    public static final IntegerProperty POWER_SOUTH = IntegerProperty.create("south_power", 0, 15);
    public static final IntegerProperty POWER_WEST = IntegerProperty.create("west_power", 0, 15);

    static {
        VoxelShape shape = Shapes.empty();

        shape = Shapes.join(shape, Block.box(1, 0, 1, 15, 2, 15), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(2, 2, 2, 14, 9, 14), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(0, 9, 0, 16, 14, 16), BooleanOp.OR);

        BLOCK_SHAPE = ShapeBuilder.shapeBuilder(shape).forDirectional(Direction.NORTH);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        return BLOCK_SHAPE.get(Direction.NORTH);
    }

    public LodestoneTrackerBlock(Properties properties){
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(POWER_NORTH, 0)
            .setValue(POWER_EAST, 0)
            .setValue(POWER_SOUTH, 0)
            .setValue(POWER_WEST, 0));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(POWER_NORTH, POWER_EAST, POWER_SOUTH, POWER_WEST);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LodestoneTrackerBlockEntity(CreatePropulsion.LODESTONE_TRACKER_BLOCK_ENTITY.get(), pos, state);
    }

    //Handling hand interactions
    @SuppressWarnings("null")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        if (ray.getDirection() != Direction.UP) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LodestoneTrackerBlockEntity trackerBE)) return InteractionResult.PASS;

        ItemStack heldStack = player.getItemInHand(hand);
        boolean trackerHasCompass = trackerBE.hasCompass();
        boolean playerHoldingCompass = heldStack.getItem() == Items.COMPASS;

        if (playerHoldingCompass && !trackerHasCompass) {
            // Player has compass, tracker is empty -> Place compass
            ItemStack compassToPlace = heldStack.split(1);
            trackerBE.setCompass(compassToPlace);
            AllSoundEvents.DEPOT_SLIDE.playOnServer(level, pos);
            return InteractionResult.CONSUME;

        } else if (heldStack.isEmpty() && trackerHasCompass) {
            // Player has empty hand, tracker has compass -> Take compass
            ItemStack takenCompass = trackerBE.removeCompass();
            player.getInventory().placeItemBackInInventory(takenCompass);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return new SmartBlockEntityTicker<>();
    }
    //Redstone

    @Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return side != Direction.DOWN && side != Direction.UP;
	}

    @Override
    public int getSignal(@Nonnull BlockState blockState, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side){
        boolean invertedDirection = true;
        if (invertedDirection) {
            if (side == Direction.NORTH) return blockState.getValue(POWER_NORTH);
            if (side == Direction.EAST) return blockState.getValue(POWER_EAST);
            if (side == Direction.SOUTH) return blockState.getValue(POWER_SOUTH);
            if (side == Direction.WEST) return blockState.getValue(POWER_WEST);
        } else {
            if (side == Direction.NORTH) return blockState.getValue(POWER_SOUTH);
            if (side == Direction.EAST) return blockState.getValue(POWER_WEST);
            if (side == Direction.SOUTH) return blockState.getValue(POWER_NORTH);
            if (side == Direction.WEST) return blockState.getValue(POWER_EAST);
        }
        return 0;
    }

    @Override
    public boolean isSignalSource(@Nonnull BlockState state){
        return  state.getValue(POWER_NORTH) > 0 || 
                state.getValue(POWER_EAST)  > 0 ||
                state.getValue(POWER_SOUTH) > 0 ||
                state.getValue(POWER_WEST)  > 0;
    }
}
