package com.deltasf.createpropulsion.compat.computercraft;

import org.joml.Math;

import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

public class OpticalSensorPeripheral extends SyncedPeripheral<OpticalSensorBlockEntity> {
    public OpticalSensorPeripheral(OpticalSensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "optical_sensor";
    }

    @LuaFunction
    public final float getDistance() {
        return blockEntity.getRaycastDistance();
    }

    @LuaFunction(mainThread = true)
    public final void setMaxDistance(int distance) {
        int clampedDistance = Math.clamp(1, blockEntity.getMaxPossibleRaycastDistance(), distance);
        blockEntity.setMaxDistance(clampedDistance);
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof OpticalSensorPeripheral otherThruster) {
            return this.blockEntity == otherThruster.blockEntity;
        }
        return false;
    }
}
