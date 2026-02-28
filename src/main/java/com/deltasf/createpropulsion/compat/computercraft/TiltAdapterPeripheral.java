package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.tilt_adapter.TiltAdapterBlockEntity;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;

public class TiltAdapterPeripheral extends SyncedPeripheral<TiltAdapterBlockEntity> {
    public TiltAdapterPeripheral(TiltAdapterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "tilt_adapter";
    }

    @LuaFunction(mainThread = true)
    public final void setAngle(double target) {
        if (target > 1.0) target = 1.0;
        if (target < -1.0) target = -1.0;

        if (Math.abs(target) < MathUtility.epsilon) {
            resetAngle();
            return;
        }

        float angleRange = PropulsionConfig.TILT_ADAPTER_ANGLE_RANGE.get().floatValue();
        blockEntity.setComputerTargetAngle((float) (target * angleRange));
    }

    @LuaFunction(mainThread = true)
    public final void resetAngle() {
        blockEntity.setComputerTargetAngle(0f);
    }
}