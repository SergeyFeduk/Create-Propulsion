package com.deltasf.createpropulsion.balloons.particles.effectors;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;

public class StreamEffector implements HapEffector {
    public float intensity = 0.0f;
    private int height = 0;
    private float invEffectiveHeight;

    private final int originX, originY, originZ;
    private final float centerX, centerY, centerZ;
    
    private final int balloonId;

    public final LongOpenHashSet occupiedBuckets = new LongOpenHashSet();

    private static final float MAX_RADIUS_SQ = 2.0f;
    private static final float INV_MAX_RADIUS_SQ = 1.0f / MAX_RADIUS_SQ;
    private static final float BASE_STRENGTH = 0.5f;

    private static final float FORCE_SOFTENING = 0.2f;
    private static final float CENTERING_STRENGTH = 0.45f;

    private static final float NEUTRAL_THRESHOLD = 0.75f; 
    private static final float CENTERING_WEAKNESS = 0.2f;
    private static final float SLOPE_PULL = 1.0f / (1.0f - NEUTRAL_THRESHOLD);
    private static final float SLOPE_PUSH = (1.0f / NEUTRAL_THRESHOLD) * CENTERING_WEAKNESS;

    public StreamEffector(BlockPos origin, double anchorX, double anchorY, double anchorZ, int balloonId) {
        this.originX = origin.getX();
        this.originY = origin.getY();
        this.originZ = origin.getZ();
        this.centerX = (float)(origin.getX() - anchorX) + 0.5f;
        this.centerY = (float)(origin.getY() - anchorY) + 0.5f;
        this.centerZ = (float)(origin.getZ() - anchorZ) + 0.5f;

        this.balloonId = balloonId;
        setHeight(0);
    }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { 
        this.height = height;
        this.invEffectiveHeight = 1.0f / Math.max(height, 1.0f);
    }
    public int getOriginX() { return originX; }
    public int getOriginY() { return originY; }
    public int getOriginZ() { return originZ; }

    @Override
    public void apply(float rx, float ry, float rz, Vector3f f) {
        final float curIntensity = intensity;
        if (curIntensity <= 0.001f) return;

        float dy = ry - centerY; 
        if (dy < -0.5f || dy > height + 0.5f) return;

        final float dx = rx - centerX;
        final float dz = rz - centerZ;
        final float distSq = dx*dx + dz*dz;
        
        if (distSq > MAX_RADIUS_SQ) return;

        float verticalFactor = 1.0f - (dy * invEffectiveHeight);
        if (verticalFactor < 0.0f) verticalFactor = 0.0f;

        final float commonFalloff = 1.0f - (distSq * INV_MAX_RADIUS_SQ);
        final float vForce = (BASE_STRENGTH * curIntensity) * commonFalloff * verticalFactor;

        float diff = verticalFactor - NEUTRAL_THRESHOLD;
        float chosenSlope = (diff > 0.0f) ? SLOPE_PULL : SLOPE_PUSH;
        float divergenceMap = diff * chosenSlope;

        float hScalar = (CENTERING_STRENGTH * curIntensity) * divergenceMap * commonFalloff;
        hScalar /= (distSq + FORCE_SOFTENING);

        f.add(-dx * hScalar, vForce, -dz * hScalar);
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
