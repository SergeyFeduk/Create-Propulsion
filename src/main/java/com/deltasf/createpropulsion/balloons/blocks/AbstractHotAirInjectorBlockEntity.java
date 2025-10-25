package com.deltasf.createpropulsion.balloons.blocks;

import java.util.List;
import java.util.UUID;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.hot_air.BalloonAttachment;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.serialization.BalloonSerializationHandler;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractHotAirInjectorBlockEntity extends SmartBlockEntity {
    public AbstractHotAirInjectorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        tryRegister();
    }

    protected UUID haiId;

    public UUID getId() { return haiId; }

    @Override
    public void onLoad() {
        super.onLoad();
        tryRegister();
    }

    public void onBalloonLoaded() {}

    @SuppressWarnings("null")
    public void tryRegister() {
        if (level == null || level.isClientSide) return;
        if (this.haiId == null) {
            this.haiId = UUID.randomUUID();
            setChanged();
        }
        //We are functional only on ship
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).registerHai(haiId, level, worldPosition);
            BalloonAttachment.ensureAttachmentExists(level, worldPosition);
            BalloonSerializationHandler.onHaiReady(ship, level);

        }
    }

    @SuppressWarnings("null")
    public void onBlockBroken() {
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null && this.haiId != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).unregisterHai(haiId, level);
        }
    }

    @SuppressWarnings("null")
    public void scan() {
        if (haiId == null || level == null || level.isClientSide()) return;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).startScanFor(haiId, level, worldPosition);
            BalloonAttachment.ensureAttachmentExists(level, worldPosition);
        }
    }

    public abstract double getInjectionAmount();

    //NBT

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
    
    public abstract void addBehaviours(List<BlockEntityBehaviour> behaviours);
}
