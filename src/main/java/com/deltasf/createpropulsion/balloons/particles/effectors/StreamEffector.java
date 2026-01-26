package com.deltasf.createpropulsion.balloons.particles.effectors;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;

public class StreamEffector implements HapEffector {
    public float intensity = 0.0f;
    private int height = 0;

    private final int originX, originY, originZ;
    private final float centerX, centerY, centerZ;
    
    private final int balloonId;

    public final LongOpenHashSet occupiedBuckets = new LongOpenHashSet();

    private static final float MAX_RADIUS_SQ = 2.0f;
    private static final float BASE_STRENGTH = 0.5f;
    private static final float CENTERING_STRENGTH = 0.125f;

    public StreamEffector(BlockPos origin, double anchorX, double anchorY, double anchorZ, int balloonId) {
        this.originX = origin.getX();
        this.originY = origin.getY();
        this.originZ = origin.getZ();
        this.centerX = (float)(origin.getX() - anchorX) + 0.5f;
        this.centerY = (float)(origin.getY() - anchorY) + 0.5f;
        this.centerZ = (float)(origin.getZ() - anchorZ) + 0.5f;

        this.balloonId = balloonId;
    }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public int getOriginX() { return originX; }
    public int getOriginY() { return originY; }
    public int getOriginZ() { return originZ; }

    @Override
    public void apply(float rx, float ry, float rz, Vector3f f) {
        if (intensity <= 0.001f) return;

        float dy = ry - centerY; 
        if (dy < -0.5f || dy > height + 0.5f) return;

        float dx = rx - centerX;
        float dz = rz - centerZ;
        float distSq = dx*dx + dz*dz;
        
        if (distSq > MAX_RADIUS_SQ) return;

        float effectiveHeight = Math.max(height, 1.0f);
        float verticalFactor = 1.0f - (dy / effectiveHeight);
        if (verticalFactor < 0.0f) verticalFactor = 0.0f;

        float falloff = 1.0f - (distSq / MAX_RADIUS_SQ);
        f.add(0, BASE_STRENGTH * intensity * falloff * verticalFactor, 0);

        //Centering for first half, divergence for the second. Lerped
        float divergenceMap = (2.0f * verticalFactor) - 1.0f;
        float hForce = CENTERING_STRENGTH * intensity * divergenceMap;
        f.add(-dx * hForce, 0, -dz * hForce);
    }

    @Override
    public int getBalloonId() {
        return balloonId;
    }

    @Override
    public boolean isOrigin(BlockPos pos) {
        return pos.getX() == originX && pos.getY() == originY && pos.getZ() == originZ;
    }
}
