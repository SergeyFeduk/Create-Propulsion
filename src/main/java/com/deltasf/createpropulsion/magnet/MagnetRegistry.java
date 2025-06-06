package com.deltasf.createpropulsion.magnet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class MagnetRegistry {
    //#region Singleton
    private static MagnetRegistry INSTANCE;
    public static MagnetRegistry get() {
        if (INSTANCE == null) INSTANCE = new MagnetRegistry();
        return INSTANCE;
    }
    private MagnetRegistry() {}
    //#endregion
    
    public static final double magnetRange = 32.0;
    public static final double magnetRangeSquared = magnetRange * magnetRange;
    public boolean debug = false;

    private final Map<ResourceKey<Level>, MagnetLevelRegistry> registries = new ConcurrentHashMap<>();

    public MagnetLevelRegistry forLevel(Level level) {
        return registries.computeIfAbsent(level.dimension(), MagnetLevelRegistry::new);
    }

    public void reset() {
        registries.clear();
    }
}
