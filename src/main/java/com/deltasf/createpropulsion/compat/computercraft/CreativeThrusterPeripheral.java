package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.thruster.AbstractThrusterBlockEntity;
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
    public final String getType() {
        return "creative_thruster";
    }

    @LuaFunction
    public final int getObstruction() {
        return blockEntity.getEmptyBlocks();
    }

    @LuaFunction(mainThread = true)
    public final void setPower(double power) {
        blockEntity.setDigitalInput((float)power);
    }

    @LuaFunction(mainThread = true)
    public final float getPower() {
        return blockEntity.getPower();
    }

    @LuaFunction(mainThread = true)
    public final void setThrustConfig(int percent) {
        blockEntity.setThrustConfig(percent);
    }

    @LuaFunction
    public final int getThrustConfig() {
        return blockEntity.getThrustConfig();
    }

    @LuaFunction
    public final float getTargetThrustKN() {
        return blockEntity.getTargetThrustNewtons() / 1000.0f;
    }

    //Boilerplate
    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof CreativeThrusterPeripheral otherThruster) {
            return this.blockEntity == otherThruster.blockEntity;
        }
        return false;
    }

    @Override
    public void attach(@NotNull IComputerAccess computer) {
        super.attach(computer);
        blockEntity.setControlMode(AbstractThrusterBlockEntity.ControlMode.PERIPHERAL);
    }

    @Override
    public void detach(@NotNull IComputerAccess computer) {
        super.detach(computer);
        blockEntity.setDigitalInput(0.0f); 
        blockEntity.setControlMode(AbstractThrusterBlockEntity.ControlMode.NORMAL);
    }
}