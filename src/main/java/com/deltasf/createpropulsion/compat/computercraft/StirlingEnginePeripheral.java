package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.heat.engine.StirlingEngineBlockEntity;
import com.deltasf.createpropulsion.heat.engine.StirlingScrollValueBehaviour;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;

public class StirlingEnginePeripheral extends SyncedPeripheral<StirlingEngineBlockEntity> {
    public StirlingEnginePeripheral(StirlingEngineBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "stirling_engine";
    }

    @LuaFunction
    public final int getRpm() {
        return blockEntity.getTargetSpeedBehaviour().getRPM();
    }

    @LuaFunction(mainThread = true)
    public final void setSpeed(int targetSpeed) {
        float step = StirlingScrollValueBehaviour.STEP;
        int value = Math.round(targetSpeed / step);

        if (value == 0) { value = targetSpeed >= 0 ? 1 : -1; }
        if (value > 4) value = 4;
        if (value < -4) value = -4;

        blockEntity.getTargetSpeedBehaviour().setValue(value);
    }

    @LuaFunction(mainThread = true)
    public final void setActive(boolean active) {
        blockEntity.setComputerActive(active);
    }
}
