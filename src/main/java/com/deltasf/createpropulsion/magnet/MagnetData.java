package com.deltasf.createpropulsion.magnet;

import org.joml.Vector3d;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class MagnetData {
    public MagnetData(BlockPos pos, long shipId, Vector3i blockDipoleDir) {
        this.pos = pos;
        this.shipId = shipId;
        this.blockDipoleDir = blockDipoleDir;
    }
    private BlockPos pos;
    public long shipId = -1;
    private Vector3d worldPosition;
    private final Vector3i blockDipoleDir; 

    public BlockPos getBlockPos() { return pos; }
    public Vector3d getPosition() { return worldPosition; }
    public Vector3i getBlockDipoleDir() { return blockDipoleDir; }

    public void updateWorldPosition(Level level) {
        if (shipId == -1) {
            worldPosition = VectorConversionsMCKt.toJOML(pos.getCenter());
        } else {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
            //TODO: ship might be null, handle that case
            worldPosition = VectorConversionsMCKt.toJOML(VSGameUtilsKt.toWorldCoordinates(ship, pos.getCenter()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MagnetData that = (MagnetData) o;
        return pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}