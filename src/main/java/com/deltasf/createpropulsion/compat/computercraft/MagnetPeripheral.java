package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.magnet.RedstoneMagnetBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;

public class MagnetPeripheral extends SyncedPeripheral<RedstoneMagnetBlockEntity> {
    public MagnetPeripheral(RedstoneMagnetBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "redstone_magnet";
    }

    //Sets the power of the magnet
    @LuaFunction(mainThread = true)
    public final void setPower(int power) {
        int clampedPower = Math.max(Math.min(power, 15), 0);
        blockEntity.overridePower = clampedPower != 0;
        blockEntity.overridenPower = clampedPower;
        blockEntity.scheduleUpdate();
        blockEntity.updateBlockstateFromPower();
        blockEntity.setChanged();
    }
}
