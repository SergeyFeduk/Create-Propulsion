package com.deltasf.createpropulsion.balloons.registries;

import java.util.Collection;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class BalloonShipRegistry {
    private static BalloonShipRegistry INSTANCE;
    public static BalloonShipRegistry get() {
        if (INSTANCE == null) INSTANCE = new BalloonShipRegistry();
        return INSTANCE;
    }
    private BalloonShipRegistry() {}

    public static final int MAX_HORIZONTAL_SCAN = 127;

    private final Long2ObjectOpenHashMap<BalloonRegistry> registries = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ServerLevel> shipIdToLevel = new Long2ObjectOpenHashMap<>();
    private final BalloonUpdater updater = new BalloonUpdater();

    public static BalloonRegistry forShip(long shipId) {
        return BalloonShipRegistry.get().registries.computeIfAbsent(shipId, k -> new BalloonRegistry());
    }

    public static BalloonRegistry forShip(long shipId, @Nonnull Level level) {
        if (level instanceof ServerLevel serverLevel) {
            get().shipIdToLevel.put(shipId, serverLevel);
        }
        return forShip(shipId);
    }

    public static BalloonUpdater updater() { return get().updater; }

    public void reset() {
        registries.clear();
    }

    public Collection<BalloonRegistry> getRegistries() {
        return registries.values();
    }

    public Long2ObjectMap<ServerLevel> getShipToLevelMap() {
        return shipIdToLevel;
    }

    public Long2ObjectMap<BalloonRegistry> getShipRegistries() {
        return registries;
    }
}
 