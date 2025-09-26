package com.deltasf.createpropulsion.heat.burners.solid;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class SolidBurnerBlock extends AbstractBurnerBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public SolidBurnerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(LIT, false));
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new SolidBurnerBlockEntity(PropulsionBlockEntities.SOLID_BURNER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HEAT, LIT);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.SOLID_BURNER_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) { 
        if (player.isSpectator() || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (world.getBlockEntity(pos) instanceof SolidBurnerBlockEntity blockEntity) {
            FuelInventoryBehaviour behaviour = blockEntity.getBehaviour(FuelInventoryBehaviour.TYPE);
            if (behaviour == null) return InteractionResult.PASS;

            if (behaviour.handlePlayerInteraction(player, hand)) return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
