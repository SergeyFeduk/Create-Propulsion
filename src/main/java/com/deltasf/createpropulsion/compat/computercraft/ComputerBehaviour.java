package com.deltasf.createpropulsion.compat.computercraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.deltasf.createpropulsion.heat.engine.StirlingEngineBlockEntity;
import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerBlockEntity;
import com.deltasf.createpropulsion.magnet.RedstoneMagnetBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlockEntity;
import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlockEntity;
import com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlockEntity;
import com.deltasf.createpropulsion.thruster.creative_thruster.CreativeThrusterBlockEntity;
import com.deltasf.createpropulsion.thruster.thruster.ThrusterBlockEntity;
import com.deltasf.createpropulsion.tilt_adapter.TiltAdapterBlockEntity;
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

    protected LazyOptional<IPeripheral> peripheral;
    protected NonNullSupplier<IPeripheral> peripheralSupplier;

    private static final Map<Class<? extends SmartBlockEntity>, Function<SmartBlockEntity, IPeripheral>> PERIPHERAL_FACTORIES = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <T extends SmartBlockEntity> void register(Class<T> clazz, Function<T, IPeripheral> factory) {
        PERIPHERAL_FACTORIES.put(clazz, be -> factory.apply((T) be));
    }

    static {
        register(ThrusterBlockEntity.class, ThrusterPeripheral::new);
        register(CreativeThrusterBlockEntity.class, CreativeThrusterPeripheral::new);
        register(OpticalSensorBlockEntity.class, OpticalSensorPeripheral::new);
        register(LodestoneTrackerBlockEntity.class, LodestoneTrackerPeripheral::new);
        register(RedstoneMagnetBlockEntity.class, MagnetPeripheral::new);
        register(PhysicsAssemblerBlockEntity.class, PhysicsAssemblerPeripheral::new);
        register(StirlingEngineBlockEntity.class, StirlingEnginePeripheral::new);
        register(RedstoneTransmissionBlockEntity.class, RedstoneTransmissionPeripheral::new);
        register(TiltAdapterBlockEntity.class, TiltAdapterPeripheral::new);
    }

    public ComputerBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
        this.peripheralSupplier = getPeripheralFor(blockEntity);
    }

    public static NonNullSupplier<IPeripheral> getPeripheralFor(SmartBlockEntity blockEntity) {
        Function<SmartBlockEntity, IPeripheral> factory = PERIPHERAL_FACTORIES.get(blockEntity.getClass());
        if (factory != null) {
            return () -> factory.apply(blockEntity);
        }

        throw new IllegalArgumentException("No peripheral available for " + ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(blockEntity.getType()));
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
