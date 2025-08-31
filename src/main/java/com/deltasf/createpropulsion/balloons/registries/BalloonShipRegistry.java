package com.deltasf.createpropulsion.balloons.registries;

import java.util.Collection;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class BalloonShipRegistry {
    private static BalloonShipRegistry INSTANCE;
    public static BalloonShipRegistry get() {
        if (INSTANCE == null) INSTANCE = new BalloonShipRegistry();
        return INSTANCE;
    }
    private BalloonShipRegistry() {}

    public static final int MAX_HORIZONTAL_SCAN = 17;

    private final Long2ObjectOpenHashMap<BalloonRegistry> registries = new Long2ObjectOpenHashMap<>();
    private final BalloonUpdater updater = new BalloonUpdater();

    public static BalloonRegistry forShip(long shipId) {
        return BalloonShipRegistry.get().registries.computeIfAbsent(shipId, k -> new BalloonRegistry());
    }

    public static BalloonUpdater updater() { return get().updater; }

    public void reset() {
        registries.clear();
    }

    public Collection<BalloonRegistry> getRegistries() {
        return registries.values();
    }
}
 