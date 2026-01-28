package com.deltasf.createpropulsion.balloons.particles;

import java.util.Map;
import java.util.Random;

import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.ClientShip;

import com.deltasf.createpropulsion.balloons.ClientBalloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.particles.effectors.EffectorBucket;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class ShipParticleHandler {
    private static final int MAX_PARTICLES = 4096;
    private static final int SAMPLE_INTERVAL = 5;

    private static final float DRAG_VOLUME = 0.95f;
    private static final float DRAG_LEAK = 0.98f;
    private static final float DRAG_STREAM = 0.99f;

    private static final float BASE_SPAWN_CHANCE = 0.33f;

    private static final float INERTIA_SCALE = 0.5f;
    private static final float LEAK_FORCE_MULTIPLIER = 0.03f;

    //Reference values for calculating stochastic amount of samples per unit of volume to maintain density of full 130 volume balloon with 2 attempts
    private static final double REFERENCE_VOLUME = 130.0;
    private static final double BASE_ATTEMPTS = 2.0;

    public final HapData data;
    public final ShipEffectorHandler effectors;
    private final ClientShipMotionAnalyzer motionAnalyzer;
    private final Random random = new Random();
    
    private double anchorX, anchorY, anchorZ;
    private boolean initialized = false;
    
    private final Vector3f tmpForce = new Vector3f();
    private int tickCounter = 0;

    public ShipParticleHandler() {
        this.data = new HapData(MAX_PARTICLES);
        this.effectors = new ShipEffectorHandler(this);
        this.motionAnalyzer = new ClientShipMotionAnalyzer();
    }
    
    public boolean isEmpty() {
        return data.count == 0;
    }
    
    public double getAnchorX() { return anchorX; }
    public double getAnchorY() { return anchorY; }
    public double getAnchorZ() { return anchorZ; }

    public void ensureInitialized(double x, double y, double z) {
        if (!initialized) {
            this.anchorX = Math.floor(x);
            this.anchorY = Math.floor(y);
            this.anchorZ = Math.floor(z);
            this.initialized = true;
        }
    }
    
    public void spawnManual(double absX, double absY, double absZ, byte state, int balloonId) {
        ensureInitialized(absX, absY, absZ);
        data.spawn((float)(absX - anchorX), (float)(absY - anchorY), (float)(absZ - anchorZ), state, balloonId);
    }

    public void spawnStream(double absX, double absY, double absZ, float vy, float vyDeviation, float life, float lifeDeviation, int balloonId) {
        ensureInitialized(absX, absY, absZ);
        int id = data.spawn((float)(absX - anchorX), (float)(absY - anchorY), (float)(absZ - anchorZ), HapData.STATE_STREAM, balloonId);
        if (id != -1) {
            data.vx[id] = (random.nextFloat() - 0.5f) * 0.05f;
            data.vy[id] = vy + (random.nextFloat() - 0.5f) * vyDeviation;
            data.vz[id] = (random.nextFloat() - 0.5f) * 0.05f;
            data.life[id] = life + (random.nextFloat() - 0.5f) * lifeDeviation;
        }
    }

    public void tick(ClientLevel level, ClientShip ship, Int2ObjectMap<ClientBalloon> allBalloons, Map<ClientBalloon, AABB> intersections) {
        tickCounter++;

        //Anchor is initialized based on the first available intersection
        if (!initialized && !intersections.isEmpty()) {
            AABB first = intersections.values().iterator().next();
            ensureInitialized(first.getCenter().x, first.getCenter().y, first.getCenter().z);
        }

        spawnVolumeParticles(intersections);

        if (data.count == 0) return;

        float dt = 0.05f;

        //Precompute inertial vectors
        motionAnalyzer.tick(ship);
        float inertiaLinearX = motionAnalyzer.linearInertia.x * INERTIA_SCALE;
        float inertiaLinearY = motionAnalyzer.linearInertia.y * INERTIA_SCALE;
        float inertiaLinearZ = motionAnalyzer.linearInertia.z * INERTIA_SCALE;
        float inertiaAngularX = motionAnalyzer.angularInertia.x * INERTIA_SCALE;
        float inertiaAngularY = motionAnalyzer.angularInertia.y * INERTIA_SCALE;
        float inertiaAngularZ = motionAnalyzer.angularInertia.z * INERTIA_SCALE;
        //Precompute leak upwards force        
        float leakForceX = motionAnalyzer.worldUpInLocal.x * LEAK_FORCE_MULTIPLIER;
        float leakForceY = motionAnalyzer.worldUpInLocal.y * LEAK_FORCE_MULTIPLIER;
        float leakForceZ = motionAnalyzer.worldUpInLocal.z * LEAK_FORCE_MULTIPLIER;

        for (int i = 0; i < data.count; i++) {
            float rx = data.x[i];
            float ry = data.y[i];
            float rz = data.z[i];
            
            double absX = anchorX + rx;
            double absY = anchorY + ry;
            double absZ = anchorZ + rz;

            boolean isSamplingTick = (i + tickCounter) % SAMPLE_INTERVAL == 0;
            
            //Wall check
            if ((i + tickCounter) % 2 == 0) {
                if (data.state[i] == HapData.STATE_VOLUME) {
                    int bId = data.balloonId[i];
                    ClientBalloon balloon = allBalloons.get(bId);
                    if (balloon == null || !balloon.volume.contains(packPos(absX, absY, absZ))) {
                        if (HaiGroup.isHab(Mth.floor(absX), Mth.floor(absY), Mth.floor(absZ), level)) {
                            data.life[i] = 0;
                        } else {
                            data.state[i] = HapData.STATE_LEAK;
                            data.life[i] += 0.2f; 
                            if (data.life[i] > 1.0f) data.life[i] = 1.0f;
                        }
                    }
                }
            }

            float fx = 0, fy = 0, fz = 0;

            fx += inertiaLinearX;
            fy += inertiaLinearY;
            fz += inertiaLinearZ;

            fx += (inertiaAngularY * rz - inertiaAngularZ * ry);
            fy += (inertiaAngularZ * rx - inertiaAngularX * rz);
            fz += (inertiaAngularX * ry - inertiaAngularY * rx);

            if (data.state[i] == HapData.STATE_LEAK) {
                fx += leakForceX;
                fy += leakForceY;
                fz += leakForceZ;
            } else if (data.state[i] == HapData.STATE_STREAM) {
                if (isSamplingTick) {
                    sampleEnvironment(i, absX, absY, absZ);
                }

                fx += data.cfx[i];
                fy += data.cfy[i];
                fz += data.cfz[i];

                int bId = data.balloonId[i];
                ClientBalloon balloon = allBalloons.get(bId);
                if (balloon != null && balloon.volume.contains(packPos(absX, absY, absZ))) {
                    data.state[i] = HapData.STATE_VOLUME;
                }
            } else {
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

            //Integration
            data.vx[i] += fx * dt;
            data.vy[i] += fy * dt;
            data.vz[i] += fz * dt;

            data.px[i] = data.x[i];
            data.py[i] = data.y[i];
            data.pz[i] = data.z[i];

            data.x[i] += data.vx[i] * dt;
            data.y[i] += data.vy[i] * dt;
            data.z[i] += data.vz[i] * dt;

            //Drag
            float drag;
            if (data.state[i] == HapData.STATE_LEAK) drag = DRAG_LEAK;
            else if (data.state[i] == HapData.STATE_STREAM) drag = DRAG_STREAM;
            else drag = DRAG_VOLUME;

            data.vx[i] *= drag;
            data.vy[i] *= drag;
            data.vz[i] *= drag;

            //Life
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
        long currentKey = packPos(bx, by, bz);

        //Check Cache
        if (data.lastBlockPosKey[i] != currentKey) {
            data.cachedBucket[i] = effectors.getOrCreateBucket(currentKey);
            data.lastBlockPosKey[i] = currentKey;
        }
        
        EffectorBucket bucket = data.cachedBucket[i];

        tmpForce.zero();

        float rx = (float)(absX - anchorX);
        float ry = (float)(absY - anchorY);
        float rz = (float)(absZ - anchorZ);

        if (bucket != null) {
            for (int k = 0; k < bucket.count; k++) {
                bucket.effectors[k].apply(rx, ry, rz, tmpForce);
            }
        }
        
        data.cfx[i] = tmpForce.x;
        data.cfy[i] = tmpForce.y;
        data.cfz[i] = tmpForce.z;
    }

    private void spawnVolumeParticles(Map<ClientBalloon, AABB> intersections) {
        if (intersections.isEmpty()) return;
        
        for (Map.Entry<ClientBalloon, AABB> entry : intersections.entrySet()) {
            if (data.count >= data.capacity) break;
            
            ClientBalloon balloon = entry.getKey();
            AABB bounds = entry.getValue();
            double spawnVolume = (bounds.maxX - bounds.minX) * (bounds.maxY - bounds.minY) * (bounds.maxZ - bounds.minZ);
            
            //Density normalization
            double targetAttempts = BASE_ATTEMPTS * (spawnVolume / REFERENCE_VOLUME);

            //Rounding done stochastically. Not perfect but does not require additional state :P
            int attempts = (int) targetAttempts;
            if (random.nextDouble() < (targetAttempts - attempts)) {
                attempts++;
            }

            if (attempts == 0) continue;
            
            float baseThreshold = balloon.getFullness() * BASE_SPAWN_CHANCE;
            for (int k = 0; k < attempts; k++) {
                if (data.count >= data.capacity) break;

                double rx = bounds.minX + (bounds.maxX - bounds.minX) * random.nextDouble();
                double ry = bounds.minY + (bounds.maxY - bounds.minY) * random.nextDouble();
                double rz = bounds.minZ + (bounds.maxZ - bounds.minZ) * random.nextDouble();
                
                boolean shouldSpawn = false;

                if (random.nextFloat() < baseThreshold) {
                    shouldSpawn = true;
                } else {
                    long packed = packPos(rx, ry, rz);
                    EffectorBucket bucket = effectors.getBucket(packed);
                    
                    if (bucket != null && bucket.hasHole) {
                        shouldSpawn = true; //Force spawn due to hole
                    }
                }

                if (shouldSpawn) {
                    long packed = packPos(rx, ry, rz); 
                    
                    if (balloon.volume.contains(packed)) {
                        int id = data.spawn((float)(rx - anchorX), (float)(ry - anchorY), (float)(rz - anchorZ), HapData.STATE_VOLUME, balloon.id);
                        if (id != -1) {
                            data.life[id] = 0.5f + random.nextFloat() * 0.5f;
                            data.x[id] += (random.nextFloat() - 0.5f) * 0.2f;
                            data.z[id] += (random.nextFloat() - 0.5f) * 0.2f;
                        }
                    }
                }
            }
        }
    }

    //TODO: Reference directly
    private static long packPos(double x, double y, double z) {
        return packPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    private static long packPos(int x, int y, int z) {
        long i = 0L;
        i |= ((long)x & PACKED_X_MASK) << X_OFFSET;
        i |= ((long)y & PACKED_Y_MASK) << Y_OFFSET;
        return i | ((long)z & PACKED_Z_MASK) << Z_OFFSET;
    }

    //BlockPos slop
    private static final int PACKED_X_LENGTH = 26;
    private static final int PACKED_Z_LENGTH = PACKED_X_LENGTH;
    private static final int PACKED_Y_LENGTH = 64 - PACKED_X_LENGTH - PACKED_Z_LENGTH;
    private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
    private static final int Y_OFFSET = 0;
    private static final int Z_OFFSET = PACKED_Y_LENGTH;
    private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;
}