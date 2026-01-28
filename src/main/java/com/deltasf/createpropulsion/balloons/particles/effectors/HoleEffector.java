package com.deltasf.createpropulsion.balloons.particles.effectors;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public class HoleEffector implements HapEffector {
    private final int balloonId;
    private final float centerX, centerY, centerZ;
    private final int originX, originY, originZ;
    
    //Normalized flow direction
    private final float dirX, dirY, dirZ;
    
    private final float strength;
    private final float radius;

    public HoleEffector(int balloonId, double anchorX, double anchorY, double anchorZ, int holeX, int holeY, int holeZ, Vector3f direction, float strength) {
        this.balloonId = balloonId;
        this.originX = holeX;
        this.originY = holeY;
        this.originZ = holeZ;
        this.centerX = (float)(holeX - anchorX) + 0.5f;
        this.centerY = (float)(holeY - anchorY) + 0.5f;
        this.centerZ = (float)(holeZ - anchorZ) + 0.5f;
        this.strength = strength;
        this.radius = 3.0f; //Max influence radius
        
        //Normalize direction
        if (direction.lengthSquared() > 0.0001f) {
            direction.normalize();
        }
        this.dirX = direction.x;
        this.dirY = direction.y;
        this.dirZ = direction.z;
    }

    @Override
    public void apply(float rx, float ry, float rz, Vector3f f) {
        float dx = centerX - rx;
        float dy = centerY - ry;
        float dz = centerZ - rz;
        float currentDistSq = dx*dx + dy*dy + dz*dz;
        if (currentDistSq > radius * radius) return;

        float nextDx = dx - dirX;
        float nextDy = dy - dirY;
        float nextDz = dz - dirZ;
        float nextDistSq = nextDx*nextDx + nextDy*nextDy + nextDz*nextDz;
        if (nextDistSq >= currentDistSq) return;

        float dist = Mth.sqrt(currentDistSq);
        float falloff = 1.0f - (dist / radius);
        if (falloff < 0) falloff = 0;
        falloff = falloff * falloff; 
        f.add(dirX * strength * falloff, dirY * strength * falloff, dirZ * strength * falloff);
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
