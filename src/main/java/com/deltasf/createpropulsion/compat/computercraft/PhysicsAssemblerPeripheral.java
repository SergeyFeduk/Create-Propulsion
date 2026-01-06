package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

public class PhysicsAssemblerPeripheral extends SyncedPeripheral<PhysicsAssemblerBlockEntity> {
    public PhysicsAssemblerPeripheral(PhysicsAssemblerBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "physics_assembler";
    }

    @LuaFunction(mainThread = true)
    public final void shipify() {
        blockEntity.shipify();
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof PhysicsAssemblerPeripheral otherAssembler) {
            return this.blockEntity == otherAssembler.blockEntity;
        }
        return false;
    }
}
