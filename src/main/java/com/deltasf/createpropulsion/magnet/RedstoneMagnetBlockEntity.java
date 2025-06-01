package com.deltasf.createpropulsion.magnet;

import java.util.List;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneMagnetBlockEntity extends SmartBlockEntity {
    public RedstoneMagnetBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        
    }
    public MagnetData data;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public void activate() {
        boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, worldPosition);
        long shipId = -1;
        if (onShip) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
            shipId = ship.getId();
        }
        if (data == null) {
            data = new MagnetData(worldPosition, shipId);
            data.updateWorldPosition(getLevel());
        }
        MagnetRegistry.get().updateMagnet(getLevel(), data);
    }

    public void deactivate() {
        MagnetRegistry.get().removeMagnet(getLevel(), data);
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        if (level.isClientSide) return;
        if (data == null) return;
        if (data.shipId == -1) return;
        if (getBlockState().getValue(RedstoneMagnetBlock.POWERED)) {
            data.updateWorldPosition(level);
        }
    }
}
