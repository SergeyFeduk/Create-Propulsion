package com.deltasf.createpropulsion.balloons.envelopes;

import java.util.function.Supplier;

import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EnvelopedShaftBlock extends AbstractEncasedShaftBlock implements EncasedBlock, IBE<KineticBlockEntity>, IEnvelope {
    private final Supplier<Block> casingBlock;

    public EnvelopedShaftBlock(Properties properties, Supplier<Block> casingBlock) {
        super(properties);
        this.casingBlock = casingBlock;
    }

    @Override
    public Block getCasing() {
        return casingBlock.get();
    }

    @Override
    public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand, BlockHitResult ray) {
        KineticBlockEntity.switchToBlockState(level, pos, defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS)));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) {
            BalloonShipRegistry.updater().habBlockPlaced(pos, level);
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        
        if (!level.isClientSide()) {
            BalloonShipRegistry.updater().habBlockRemoved(pos, level);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<KineticBlockEntity> getBlockEntityClass() {
        return KineticBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.ENVELOPED_SHAFT.get();
    }

    @Override
    public boolean isEnvelope() { return true; }
}
