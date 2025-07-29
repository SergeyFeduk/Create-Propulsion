package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

public class LodestoneTrackerPeripheral extends SyncedPeripheral<LodestoneTrackerBlockEntity> {
    public LodestoneTrackerPeripheral(LodestoneTrackerBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "lodestone_tracker";
    }

    @LuaFunction
    public float getAngle() {
        return blockEntity.getAngle();
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof LodestoneTrackerPeripheral otherTracker) {
            return this.blockEntity == otherTracker.blockEntity;
        }
        return false;
    }
}
