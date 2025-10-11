package com.deltasf.createpropulsion.reaction_wheel;

import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import net.minecraft.core.BlockPos;

public class ReactionWheelForceApplier {
    private ReactionWheelData data;

    public ReactionWheelForceApplier(ReactionWheelData data) {
        this.data = data;
    }

    public void applyForces(BlockPos pos, PhysShipImpl ship) {
    }
}

