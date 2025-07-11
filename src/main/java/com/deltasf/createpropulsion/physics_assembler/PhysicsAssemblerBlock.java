package com.deltasf.createpropulsion.physics_assembler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

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
