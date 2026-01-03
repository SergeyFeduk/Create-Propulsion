package com.deltasf.createpropulsion.balloons.events;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;

import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.Event;

public class BalloonVolumeChangeEvent extends Event {
    public enum Type {
        CREATED,
        DESTROYED,

        EXTENDED,
        SHRUNK,

        MERGED,
        SPLIT, //Emitted by balloons AFFECTED BY SPLIT

        HOLE_CREATED,
        HOLE_REMOVED
    }

    private final Balloon balloon;
    private final AABB affectedBounds;
    private final Type type;
    private final BalloonRegistry registry;

    public BalloonVolumeChangeEvent(Balloon balloon, AABB affectedBounds, Type type, BalloonRegistry registry) {
        this.balloon = balloon;
        this.affectedBounds = affectedBounds;
        this.type = type;
        this.registry = registry;
    }

    public Balloon getBalloon() {
        return balloon;
    }

    public AABB getAffectedBounds() {
        return affectedBounds;
    }

    public Type getType() {
        return type;
    }

    public BalloonRegistry getRegistry() {
        return registry;
    }
}
