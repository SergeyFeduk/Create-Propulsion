package com.deltasf.createpropulsion.heat.burners.liquid;

import java.util.List;

import com.deltasf.createpropulsion.heat.HeatMapper.HeatLevelString;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.burners.BurnerDamager;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LiquidBurnerBlockEntity extends AbstractBurnerBlockEntity {
    private HeatLevelString heatLevelName = HeatLevelString.COLD;
    private boolean isPowered = false;
    private BurnerDamager damager;

    private static final float MAX_HEAT = 400.0f;
    private static final float PASSIVE_LOSS_PER_TICK = 0.05f;

    public LiquidBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        
        damager = new BurnerDamager(this);
        behaviours.add(damager);
    }

    public float getHeatPerTick() { return 2; }

    @SuppressWarnings("null")
    public void updatePoweredState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean currentlyPowered = level.getBestNeighborSignal(worldPosition) > 0;
        if (this.isPowered != currentlyPowered) {
            this.isPowered = currentlyPowered;
            notifyUpdate();
        }
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide()) return;
    }

    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }

    @Override
    protected float getBaseHeatCapacity() {
        return MAX_HEAT;
    }
}
