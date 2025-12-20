package com.deltasf.createpropulsion.heat.burners.liquid;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("deprecation")
public class LiquidBurnerBlock extends AbstractBurnerBlock {
    public LiquidBurnerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any());
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = player.isShiftKeyDown() ? baseDirection.getOpposite() : baseDirection;
        } else {
            placeDirection = baseDirection;
        }

        return this.defaultBlockState().setValue(FACING, placeDirection);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LiquidBurnerBlockEntity(PropulsionBlockEntities.LIQUID_BURNER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.LIQUID_BURNER_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }

    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide()) return;
        
        if (level.getBlockEntity(pos) instanceof LiquidBurnerBlockEntity burner) {
            burner.updatePoweredState();
            burner.updatePipeCapability(); 
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);
            if (neighborBE != null) {
                neighborBE.setChanged(); 
            }
        }
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide()) return;

        if (level.getBlockEntity(pos) instanceof LiquidBurnerBlockEntity burner) {
            burner.updatePoweredState();
            burner.updatePipeCapability();
        }
    }
}