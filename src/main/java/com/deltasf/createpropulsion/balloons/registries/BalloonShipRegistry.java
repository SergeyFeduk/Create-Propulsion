package com.deltasf.createpropulsion.balloons.registries;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

//This class is used to manage all ships and redirect access to the correct per-ship handler which is BalloonRegistry
public class BalloonShipRegistry {
    private static BalloonShipRegistry INSTANCE;
    public static BalloonShipRegistry get() {
        if (INSTANCE == null) INSTANCE = new BalloonShipRegistry();
        return INSTANCE;
    }
    private BalloonShipRegistry() {}

    public static final int MAX_HORIZONTAL_SCAN = 17;

    //We use long2obj hashmap as our ships have id's which are stored as longs. So key is still a ship's id
    private final Long2ObjectOpenHashMap<BalloonRegistry> registries = new Long2ObjectOpenHashMap<>();

    public static BalloonRegistry forShip(long shipId) {
        return BalloonShipRegistry.get().registries.computeIfAbsent(shipId, k -> new BalloonRegistry());
    }

    public void reset() {
        registries.clear();
    }

    public Collection<BalloonRegistry> getRegistries() {
        return registries.values();
    }
}
