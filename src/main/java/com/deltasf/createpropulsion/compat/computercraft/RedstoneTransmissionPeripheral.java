package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlockEntity;
import com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlockEntity.TransmissionMode;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public class RedstoneTransmissionPeripheral extends SyncedPeripheral<RedstoneTransmissionBlockEntity>  {
    public RedstoneTransmissionPeripheral(RedstoneTransmissionBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "redstone_transmission";
    }

    @LuaFunction
    public final String getTransmissionMode() {
        return blockEntity.getTransmissionMode().name().toLowerCase();
    }

    @LuaFunction(mainThread = true)
    public final void setTransmissionMode(String mode) throws LuaException {
        try {
            TransmissionMode newMode = TransmissionMode.valueOf(mode.toUpperCase());
            blockEntity.setTransmissionMode(newMode);
        } catch (IllegalArgumentException e) {
            throw new LuaException("Invalid mode. Expected 'direct' or 'incremental'");
        }
    }

    @LuaFunction
    public final int getShiftLevel() {
        TransmissionMode mode = blockEntity.getTransmissionMode();
        if (mode == TransmissionMode.DIRECT) {
            return Math.round((float) blockEntity.get_current_shift() / RedstoneTransmissionBlockEntity.MAX_VALUE * 15);
        } else {
            return blockEntity.get_current_shift();
        }
    }

    @LuaFunction(mainThread = true)
    public final void setShiftLevel(int level) {
        blockEntity.setShiftFromPeripheral(level);
    }

    //TODO: Add attach/detach and store state for peripheral control in be itself similarly to both thrusters
}
