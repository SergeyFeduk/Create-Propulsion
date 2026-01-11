package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionDisplaySource;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

public class PropulsionDisplaySources {
    private static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    public static final RegistryEntry<RedstoneTransmissionDisplaySource> REDSTONE_TRANSMISSION = REGISTRATE.displaySource("redstone_transmission", RedstoneTransmissionDisplaySource::new).register();
}
