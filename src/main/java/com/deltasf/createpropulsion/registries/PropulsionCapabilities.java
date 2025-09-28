package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.heat.IHeatConsumer;
import com.deltasf.createpropulsion.heat.IHeatSource;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class PropulsionCapabilities {
    public static final Capability<IHeatSource> HEAT_SOURCE = CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<IHeatConsumer> HEAT_CONSUMER = CapabilityManager.get(new CapabilityToken<>() {});
}
