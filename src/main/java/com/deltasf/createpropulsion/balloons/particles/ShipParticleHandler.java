package com.deltasf.createpropulsion.balloons.particles;

import java.util.List;
import java.util.Random;

import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.ClientShip;

import com.deltasf.createpropulsion.balloons.ClientBalloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class ShipParticleHandler {
    private static final int MAX_PARTICLES = 4096;
    private static final int SAMPLE_INTERVAL = 5;

    private static final float HOLE_EJECT_STRENGTH = 1.8f;
    private static final float DRAG_VOLUME = 0.90f;
    private static final float DRAG_LEAK = 0.98f; 

    public final HapData data;
    private final Long2ObjectOpenHashMap<EffectorBucket> effectors = new Long2ObjectOpenHashMap<>();
    private final Random random = new Random();
    
    private double anchorX, anchorY, anchorZ;
    private boolean initialized = false;
    
    private final Vector3f tmpForce = new Vector3f();
    private int tickCounter = 0;

    public ShipParticleHandler() {
        this.data = new HapData(MAX_PARTICLES);
    }
    
    public boolean isEmpty() {
        return data.count == 0;
    }
    
    public double getAnchorX() { return anchorX; }
    public double getAnchorY() { return anchorY; }
    public double getAnchorZ() { return anchorZ; }

    private void ensureInitialized(double x, double y, double z) {
        if (!initialized) {
            this.anchorX = Math.floor(x);
            this.anchorY = Math.floor(y);
            this.anchorZ = Math.floor(z);
            this.initialized = true;
        }
    }

    public void addEffector(BlockPos pos, HapEffector effector) {
        long key = pos.asLong();
        EffectorBucket bucket = effectors.get(key);
        if (bucket == null) {
            bucket = new EffectorBucket();
            effectors.put(key, bucket);
        }
        bucket.add(effector);
    }

    public void removeEffectorsForBalloon(int balloonId) {
        // Iterate all buckets and remove effectors matching this ID
        // Note: This iterates the map. Since effectors are sparse, this is acceptable for an event-based update.
        var iter = effectors.values().iterator();
        while (iter.hasNext()) {
            EffectorBucket bucket = iter.next();
            bucket.removeByBalloonId(balloonId);
            if (bucket.isEmpty()) {
                iter.remove();
            }
        }
    }

    public void updateHoleEffectors(Level level, ClientBalloon balloon) {
        // 1. Clean up old effectors
        removeEffectorsForBalloon(balloon.id);

        if (balloon.holes.isEmpty()) return;
        
        if (!initialized && !balloon.volume.isEmpty()) {
            BlockPos first = BlockPos.of(balloon.volume.iterator().nextLong());
            ensureInitialized(first.getX(), first.getY(), first.getZ());
        }

        Vector3f dirAccumulator = new Vector3f();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        int radius = 2; // 5x5x5 area

        // 2. Calculate Direction per hole
        for (BlockPos hole : balloon.holes) {
            dirAccumulator.zero();
            
            for (Direction d : Direction.values()) {
                neighborPos.setWithOffset(hole, d);
                long neighborPacked = neighborPos.asLong();
                
                // Rule 2: Volume block - Push AWAY (Inverse of D)
                if (balloon.volume.contains(neighborPacked)) {
                    dirAccumulator.add(-d.getStepX() * 1.0f, -d.getStepY() * 1.0f, -d.getStepZ() * 1.0f);
                } 
                // Rule 1: Another Hole - Pull slightly
                else if (balloon.holes.contains(neighborPos)) {
                    dirAccumulator.add(d.getStepX() * 0.1f, d.getStepY() * 0.1f, d.getStepZ() * 0.1f);
                } 
                // Rule 3 & 4: Check World Block
                else {
                    boolean isEnvelope = HaiGroup.isHab(neighborPos, level);
                    
                    // Rule 4: Envelope - No effect
                    if (!isEnvelope) {
                        // Rule 3: Air/Obstruction - Pull towards
                        dirAccumulator.add(d.getStepX() * 1.0f, d.getStepY() * 1.0f, d.getStepZ() * 1.0f);
                    }
                }
            }

            // Only register if we have a valid flow direction
            if (dirAccumulator.lengthSquared() > 0.001f) {
                HapEffector holeEffector = new HapEffector.HoleEffector(
                    balloon.id,
                    anchorX, anchorY, anchorZ,
                    hole.getX(), hole.getY(), hole.getZ(),
                    dirAccumulator, // Pass the vector, ctor normalizes it
                    HOLE_EJECT_STRENGTH
                );

                // Register in 5x5 area
                BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            mut.set(hole.getX() + x, hole.getY() + y, hole.getZ() + z);
                            addEffector(mut, holeEffector);
                        }
                    }
                }
            }
        }
    }

    
    public void spawnManual(double absX, double absY, double absZ, byte state) {
        ensureInitialized(absX, absY, absZ);
        data.spawn((float)(absX - anchorX), (float)(absY - anchorY), (float)(absZ - anchorZ), state);
    }

    public void tick(ClientShip ship, List<ClientBalloon> intersectingBalloons, AABB intersectionBounds) {
        tickCounter++;
        spawnVolumeParticles(intersectingBalloons, intersectionBounds);

        if (data.count == 0) return;

        float dt = 0.05f; 

        for (int i = 0; i < data.count; i++) {
            float rx = data.x[i];
            float ry = data.y[i];
            float rz = data.z[i];
            
            double absX = anchorX + rx;
            double absY = anchorY + ry;
            double absZ = anchorZ + rz;

            boolean isSamplingTick = (i + tickCounter) % SAMPLE_INTERVAL == 0;
            
            // --- Wall Check ---
            // If particle hits the hole block, isInsideAnyBalloon returns false 
            // (hole is removed from volume), triggering transition.
            if ((i + tickCounter) % 2 == 0) {
                 if (data.state[i] == HapData.STATE_VOLUME) {
                    
                    if (!isInsideAnyBalloon(intersectingBalloons, absX, absY, absZ)) {
                        data.state[i] = HapData.STATE_LEAK;
                        
                        // Increase life on leak so they drift visible distance
                        data.life[i] += 0.2f; 
                        if (data.life[i] > 1.0f) data.life[i] = 1.0f;
                    }
                 }
            }

            float fx = 0, fy = 0, fz = 0;

            if (data.state[i] == HapData.STATE_LEAK) {
                // LEAK: No effectors, just simple rise
                fy += 0.02f;
            } else {
                // VOLUME: Effectors + Turbulence
                if (isSamplingTick) {
                    sampleEnvironment(i, absX, absY, absZ);
                }
                
                fx += data.cfx[i];
                fy += data.cfy[i];
                fz += data.cfz[i];

                fx += (random.nextFloat() - 0.5f) * 0.01f;
                fy += (random.nextFloat() - 0.5f) * 0.01f;
                fz += (random.nextFloat() - 0.5f) * 0.01f;
            }

            // Integration
            data.vx[i] += fx * dt;
            data.vy[i] += fy * dt;
            data.vz[i] += fz * dt;

            data.px[i] = data.x[i];
            data.py[i] = data.y[i];
            data.pz[i] = data.z[i];

            data.x[i] += data.vx[i] * dt;
            data.y[i] += data.vy[i] * dt;
            data.z[i] += data.vz[i] * dt;

            // Drag
            float drag = (data.state[i] == HapData.STATE_LEAK) ? DRAG_LEAK : DRAG_VOLUME;
            data.vx[i] *= drag;
            data.vy[i] *= drag;
            data.vz[i] *= drag;

            // Life
            data.life[i] -= 0.005f;
            if (data.life[i] <= 0) {
                data.remove(i);
                i--;
            }
        }
    }


    private void sampleEnvironment(int i, double absX, double absY, double absZ) {
        int bx = Mth.floor(absX);
        int by = Mth.floor(absY);
        int bz = Mth.floor(absZ);

        tmpForce.zero();
        
        float rx = (float)(absX - anchorX);
        float ry = (float)(absY - anchorY);
        float rz = (float)(absZ - anchorZ);

        checkEffectorAt(bx, by, bz, rx, ry, rz, tmpForce);
        
        data.cfx[i] = tmpForce.x;
        data.cfy[i] = tmpForce.y;
        data.cfz[i] = tmpForce.z;
    }
    
    private void checkEffectorAt(int x, int y, int z, float rx, float ry, float rz, Vector3f acc) {
        long key = BlockPos.asLong(x, y, z);
        EffectorBucket bucket = effectors.get(key);
        if (bucket != null) {
            for (int i = 0; i < bucket.count; i++) {
                bucket.effectors[i].apply(rx, ry, rz, acc);
            }
        }
    }

    private void spawnVolumeParticles(List<ClientBalloon> balloons, AABB intersectionBounds) {
        if (balloons.isEmpty() || intersectionBounds == null) return;
        
        ensureInitialized(intersectionBounds.getCenter().x, intersectionBounds.getCenter().y, intersectionBounds.getCenter().z);

        int spawnAttempts = 5; 
        
        for (int k = 0; k < spawnAttempts; k++) {
            if (data.count >= data.capacity) break;

            double rx = intersectionBounds.minX + (intersectionBounds.maxX - intersectionBounds.minX) * random.nextDouble();
            double ry = intersectionBounds.minY + (intersectionBounds.maxY - intersectionBounds.minY) * random.nextDouble();
            double rz = intersectionBounds.minZ + (intersectionBounds.maxZ - intersectionBounds.minZ) * random.nextDouble();
            
            if (isInsideAnyBalloon(balloons, rx, ry, rz)) {
                // Store Relative
                int id = data.spawn((float)(rx - anchorX), (float)(ry - anchorY), (float)(rz - anchorZ), HapData.STATE_VOLUME);
                if (id != -1) {
                    data.life[id] = 0.5f + random.nextFloat() * 0.5f;
                    data.x[id] += (random.nextFloat() - 0.5f);
                    data.z[id] += (random.nextFloat() - 0.5f);
                }
            }
        }
    }

    //TODO: OMFG, DO NOT ITERATE
    private boolean isInsideAnyBalloon(List<ClientBalloon> balloons, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        long packed = pos.asLong();
        for (ClientBalloon b : balloons) {
            if (b.volume.contains(packed)) return true;
        }
        return false;
    }
    
    private static class EffectorBucket {
        final HapEffector[] effectors = new HapEffector[16];
        int count = 0;
        
        EffectorBucket() {}
        
        void add(HapEffector e) {
            if (count < 6) {
                effectors[count++] = e;
            }
        }
        
        void removeByBalloonId(int id) {
            for (int i = 0; i < count; i++) {
                if (effectors[i].getBalloonId() == id) {
                    int last = count - 1;
                    if (i != last) {
                        effectors[i] = effectors[last];
                    }
                    effectors[last] = null;
                    count--;
                    i--;
                }
            }
        }
        
        boolean isEmpty() {
            return count == 0;
        }
    }
}