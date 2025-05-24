package com.deltasf.createpropulsion.physics_assembler;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class PhysicsAssemblerBlock extends DirectionalBlock implements EntityBlock {
    public static final IntegerProperty POWER = IntegerProperty.create("redstone_power", 0, 15);
    public PhysicsAssemblerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
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
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWER);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new PhysicsAssemblerBlockEntity(PropulsionBlockEntities.PHYSICAL_ASSEMBLER_BLOCK_ENTITY.get(), pos, state);
    }

    //Handle redstone signal
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide()) return;
        doRedstoneCheck(level, state, pos);
    }

    private void doRedstoneCheck(Level level, BlockState state, BlockPos pos){
        //Get redstone powers
        int oldRedstonePower = state.getValue(POWER);
        int newRedstonePower = level.getBestNeighborSignal(pos);
        if (oldRedstonePower == newRedstonePower) return; 
        //Update state
        BlockState newState = state.setValue(POWER, newRedstonePower);
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        if (oldRedstonePower != 0 || newRedstonePower == 0) return; //We were turned off And new signal is not 0
        
        //Invoke shipify
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PhysicsAssemblerBlockEntity physicsAssemblerBlockEntity) {
            physicsAssemblerBlockEntity.shipify();
        }
    }
}
