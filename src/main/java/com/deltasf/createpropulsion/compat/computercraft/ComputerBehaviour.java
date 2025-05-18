package com.deltasf.createpropulsion.compat.computercraft;

import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlockEntity;
import com.deltasf.createpropulsion.thruster.ThrusterBlockEntity;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.registries.ForgeRegistries;

public class ComputerBehaviour extends AbstractComputerBehaviour {
    protected static final Capability<IPeripheral> PERIPHERAL_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

    LazyOptional<IPeripheral> peripheral;
    NonNullSupplier<IPeripheral> peripheralSupplier;

    public ComputerBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
        this.peripheralSupplier = getPeripheralFor(blockEntity);
    }

    public static NonNullSupplier<IPeripheral> getPeripheralFor(SmartBlockEntity blockEntity) {
        if (blockEntity instanceof ThrusterBlockEntity thruster)
            return () -> new ThrusterPeripheral(thruster);
        if (blockEntity instanceof OpticalSensorBlockEntity opticalSensor)
            return () -> new OpticalSensorPeripheral(opticalSensor);
        if (blockEntity instanceof LodestoneTrackerBlockEntity tracker)
            return () -> new LodestoneTrackerPeripheral(tracker);
        throw new IllegalArgumentException(
                "No peripheral available for " + ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(blockEntity.getType()));
    }

    @Override
    public <T> boolean isPeripheralCap(Capability<T> cap) {
        return cap == PERIPHERAL_CAPABILITY;
    }

    @Override
    public <T> LazyOptional<T> getPeripheralCapability() {
        if (peripheral == null || !peripheral.isPresent())
            peripheral = LazyOptional.of(peripheralSupplier);
        return peripheral.cast();
    }

    @Override
    public void removePeripheral() {
        if (peripheral != null)
            peripheral.invalidate();
    }
}
