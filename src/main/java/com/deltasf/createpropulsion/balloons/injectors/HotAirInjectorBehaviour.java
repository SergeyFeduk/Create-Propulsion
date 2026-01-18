package com.deltasf.createpropulsion.balloons.injectors;

import java.util.UUID;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.hot_air.BalloonAttachment;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class HotAirInjectorBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<HotAirInjectorBehaviour> TYPE = new BehaviourType<>();

    private UUID haiId;

    public HotAirInjectorBehaviour(SmartBlockEntity be) {
        super(be);
    }

    public UUID getId() {
        if (haiId == null) {
            haiId = UUID.randomUUID();
            blockEntity.setChanged();
        }
        return haiId;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!getWorld().isClientSide) {
            tryRegister();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        unregister();
    }

    public void tryRegister() {
        Level level = getWorld();
        BlockPos pos = getPos();
        if (level == null || level.isClientSide) return;

        // Ensure ID exists
        getId(); 

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).registerHai(haiId, level, pos);
            BalloonAttachment.ensureAttachmentExists(level, pos);
        }
    }

    private void unregister() {
        Level level = getWorld();
        if (level == null || level.isClientSide) return;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, getPos());
        if (ship != null && this.haiId != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).unregisterHai(haiId, level);
        }
    }

    public void performScan() {
        Level level = getWorld();
        BlockPos pos = getPos();
        if (haiId == null || level == null || level.isClientSide()) return;
        
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).startScanFor(haiId, level, pos);
            BalloonAttachment.ensureAttachmentExists(level, pos);
        }
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if (nbt.hasUUID("id")) {
            this.haiId = nbt.getUUID("id");
        }
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if (this.haiId != null) {
            nbt.putUUID("id", this.haiId);
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
