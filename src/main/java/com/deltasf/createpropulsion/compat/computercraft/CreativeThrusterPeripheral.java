package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.thruster.creative_thruster.CreativeThrusterBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.NotNull;

public class CreativeThrusterPeripheral extends SyncedPeripheral<CreativeThrusterBlockEntity> {

    public CreativeThrusterPeripheral(CreativeThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "creative_thruster";
    }

    @LuaFunction
    public int getObstruction() {
        return blockEntity.getEmptyBlocks();
    }

    @LuaFunction(mainThread = true)
    public void setPower(int power) {
        int clampedPower = Math.max(Math.min(power, 15), 0);
        
        if (blockEntity.overridenPower != clampedPower || blockEntity.overridePower != (clampedPower != 0)) {
            blockEntity.overridePower = clampedPower != 0;
            blockEntity.overridenPower = clampedPower;
            blockEntity.dirtyThrust();
            blockEntity.setChanged();
            blockEntity.sendData();
        }
    }

    @LuaFunction
    public int getPower() {
        return blockEntity.getOverriddenPowerOrState(blockEntity.getBlockState());
    }

    @LuaFunction(mainThread = true)
    public void setThrustConfig(int percent) {
        blockEntity.setThrustConfig(percent);
    }

    @LuaFunction
    public int getThrustConfig() {
        return blockEntity.getThrustConfig();
    }

    @LuaFunction
    public float getTargetThrustKN() {
        return blockEntity.getTargetThrustNewtons() / 1000.0f;
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof CreativeThrusterPeripheral otherThruster) {
            return this.blockEntity == otherThruster.blockEntity;
        }
        return false;
    }

    @Override
    public void detach(@NotNull IComputerAccess computer) {
        if (blockEntity.overridePower) {
            blockEntity.overridePower = false;
            blockEntity.overridenPower = 0;
            blockEntity.dirtyThrust();
            blockEntity.setChanged();
            blockEntity.sendData();
        }
        super.detach(computer);
    }
}