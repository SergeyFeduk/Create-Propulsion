package com.deltasf.createpropulsion.compat.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.deltasf.createpropulsion.thruster.thruster.ThrusterBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

public class ThrusterPeripheral extends SyncedPeripheral<ThrusterBlockEntity> {
    private final FluidMethods fluidMethods = new FluidMethods();

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
        
        if (blockEntity.overridenPower != clampedPower || blockEntity.overridePower != (clampedPower != 0)) {
            blockEntity.overridePower = clampedPower != 0;
            blockEntity.overridenPower = clampedPower;
            blockEntity.dirtyThrust();
            blockEntity.setChanged();
            blockEntity.sendData();
        }
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

    //IFluidHandler methods passthrough
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> tanks() throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.tanks(handler);
    }

    @LuaFunction(mainThread = true)
    public int pushFluid(IComputerAccess computer, String toName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.pushFluid(handler, computer, toName, limit, fluidName);
    }

    @LuaFunction(mainThread = true)
    public int pullFluid(IComputerAccess computer, String fromName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.pullFluid(handler, computer, fromName, limit, fluidName);
    }

    private IFluidHandler getHandler() throws LuaException {
        return blockEntity.tank.getCapability()
            .orElseThrow(() -> new LuaException("Fluid tank not available"));
    }

    //Boilerplate
    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof ThrusterPeripheral otherThruster) {
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
