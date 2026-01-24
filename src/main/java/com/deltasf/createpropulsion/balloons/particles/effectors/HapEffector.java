package com.deltasf.createpropulsion.balloons.particles.effectors;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;

public interface HapEffector {
    void apply(float rx, float ry, float rz, Vector3f forceAccumulator);
    int getBalloonId();
    boolean isOrigin(BlockPos pos);
}