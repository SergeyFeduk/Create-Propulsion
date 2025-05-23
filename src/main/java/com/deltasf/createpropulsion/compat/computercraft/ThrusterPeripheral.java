package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.thruster.ThrusterBlockEntity;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

public class ThrusterPeripheral extends SyncedPeripheral<ThrusterBlockEntity> {
    public ThrusterPeripheral(ThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "propulsion_thruster";
    }

    //Returns emptyBlocks
    @LuaFunction
    public int getObstruction() {
        return blockEntity.getEmptyBlocks();
    }

    //Sets power for thruster
    @LuaFunction(mainThread = true)
    public void setPower(int power) {
        int clampedPower = Math.max(Math.min(power, 15), 0);
        blockEntity.overridePower = clampedPower == 0 ? false : true;
        blockEntity.overridenPower = clampedPower;
    }

    //Get name of the current fuel
    @LuaFunction(mainThread = true)
    public String getFuelName() {
        if (blockEntity.fluidStack().isEmpty()) return "";
        return blockEntity.fluidStack().getDisplayName().getString();
    }

    //Get thrust multiplier of current fuel
    @LuaFunction(mainThread = true)
    public float getFuelThrustMultiplier() {
        if (!blockEntity.validFluid()) return 0;
        return blockEntity.getFuelProperties(blockEntity.fluidStack().getRawFluid()).thrustMultiplier;
    }

    //Get consumption multiplier of current fuel
    @LuaFunction(mainThread = true)
    public float getFuelConsumptionMultiplier() {
        if (!blockEntity.validFluid()) return 0;
        return blockEntity.getFuelProperties(blockEntity.fluidStack().getRawFluid()).consumptionMultiplier;
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof ThrusterPeripheral otherThruster) {
            return this.blockEntity == otherThruster.blockEntity;
        }
        return false;
    }
}
