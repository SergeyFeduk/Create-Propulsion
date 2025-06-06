package com.deltasf.createpropulsion.magnet;

import org.joml.Vector3d;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.UUID;

public class MagnetData {
    public MagnetData(UUID id, BlockPos pos, long shipId, Vector3i blockDipoleDir) {
        this.id = id;
        this.pos = pos;
        this.shipId = shipId;
        this.blockDipoleDir = blockDipoleDir;
    }
    public final UUID id;
    private boolean pendingRemoval = false;

    private BlockPos pos;
    public long shipId = -1;
    private Vector3d worldPosition;
    private final Vector3i blockDipoleDir; 

    public BlockPos getBlockPos() { return pos; }
    public Vector3d getPosition() { return worldPosition; }
    public Vector3i getBlockDipoleDir() { return blockDipoleDir; }

    public void update(BlockPos newPos, long newShipId, Vector3i newBlockDipoleDir) {
        this.pos = newPos;
        this.shipId = newShipId;
        this.blockDipoleDir.set(newBlockDipoleDir);
    }

    public void updateWorldPosition(Level level) {
        if (shipId == -1) {
            worldPosition = VectorConversionsMCKt.toJOML(pos.getCenter());
        } else {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
            if (ship == null) return;
            worldPosition = VectorConversionsMCKt.toJOML(VSGameUtilsKt.toWorldCoordinates(ship, pos.getCenter()));
        }
    }

    public void scheduleForRemoval() { this.pendingRemoval = true; }
    public void cancelRemoval() { this.pendingRemoval = false; }
    public boolean isPendingRemoval() { return this.pendingRemoval; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MagnetData that = (MagnetData) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}