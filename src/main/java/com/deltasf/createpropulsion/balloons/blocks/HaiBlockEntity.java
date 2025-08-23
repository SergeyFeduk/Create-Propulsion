package com.deltasf.createpropulsion.balloons.blocks;

import java.util.List;
import java.util.UUID;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("null")
public class HaiBlockEntity extends SmartBlockEntity {
    
    public HaiBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        tryRegister();
    }

    private UUID haiId;

    public void tryRegister() {
        if (level == null || level.isClientSide) return;
        if (this.haiId == null) {
            this.haiId = UUID.randomUUID();
            setChanged();
        }
        //We are functional only on ship
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            BalloonShipRegistry.forShip(ship.getId()).registerHai(haiId, this);
        }
    }

    public void onBlockBroken() {
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null && this.haiId != null) {
            BalloonShipRegistry.forShip(ship.getId()).unregisterHai(haiId, level);
        }
    }

    public void scan() {
        if (this.haiId == null || this.level == null || this.level.isClientSide()) return;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            // FIX: Pass the level instance down the call chain.
            BalloonShipRegistry.forShip(ship.getId()).startScanFor(haiId, this.level, worldPosition);
        }
    }


    @Override
    public void onLoad() {
        super.onLoad();
        tryRegister();
    }

    @Override
    protected void read(CompoundTag tag, boolean isClient) {
        super.read(tag, isClient);
        if (tag.hasUUID("id")) {
            this.haiId = tag.getUUID("id");
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean isClient) {
        super.write(tag, isClient);
        if (this.haiId != null) {
            tag.putUUID("id", this.haiId);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
}
