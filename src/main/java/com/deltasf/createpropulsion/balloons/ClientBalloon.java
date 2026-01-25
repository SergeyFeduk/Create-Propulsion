package com.deltasf.createpropulsion.balloons;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class ClientBalloon {
    public float hotAir = 0.0f;

    public final int id;
    public final LongOpenHashSet volume = new LongOpenHashSet();
    public final Set<BlockPos> holes = new HashSet<>();
    
    public final Set<UUID> connectedHais = new HashSet<>();

    //Incremental bounds
    private final Int2IntMap countAtX = new Int2IntOpenHashMap();
    private final Int2IntMap countAtY = new Int2IntOpenHashMap();
    private final Int2IntMap countAtZ = new Int2IntOpenHashMap();

    private int minX, maxX, minY, maxY, minZ, maxZ;
    private boolean boundsInitialized = false;
    private AABB cachedBounds = null;

    public ClientBalloon(int id) {
        this.id = id;
    }

    public float getFullness() {
        if (volume.size() == 0) return 0;
        return hotAir / volume.size();
    }

    public AABB getBounds() {
        if (cachedBounds == null) updateBoundsCache();
        return cachedBounds;
    }

    public void applyDelta(long[] added, long[] removed, long[] addedHoles, long[] removedHoles, UUID[] addedHais, UUID[] removedHais) {
        for (long p : removed) {
            if (volume.remove(p)) {
                onRemoveCoords(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p));
            }
        }

        for (long p : added) {
            if (volume.add(p)) {
                onAddCoords(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p));
            }
        }
        
        for (long p : removedHoles) holes.remove(BlockPos.of(p));
        for (long p : addedHoles) holes.add(BlockPos.of(p));

        if (removedHais != null) {
            for (UUID uid : removedHais) {
                if (connectedHais.remove(uid)) {
                    ClientBalloonRegistry.updateHaiIndex(uid, -1);
                }
            }
        }
        
        if (addedHais != null) {
            for (UUID uid : addedHais) {
                if (connectedHais.add(uid)) {
                    ClientBalloonRegistry.updateHaiIndex(uid, this.id);
                }
            }
        }

    }
    
    public void setContent(long[] newVolume, long[] newHoles, UUID[] newHais) {
        volume.clear();
        holes.clear();

        for(UUID uid : connectedHais) {
            ClientBalloonRegistry.updateHaiIndex(uid, -1);
        }

        connectedHais.clear();
        
        countAtX.clear();
        countAtY.clear();
        countAtZ.clear();
        boundsInitialized = false;
        
        for(long p : newVolume) {
            volume.add(p);
            onAddCoords(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p));
        }
        
        for(long p : newHoles) holes.add(BlockPos.of(p));

        for(UUID h : newHais) connectedHais.add(h);

        for(UUID uid : connectedHais) {
            ClientBalloonRegistry.updateHaiIndex(uid, this.id);
        }
        
        updateBoundsCache();
    }

    private void onAddCoords(int x, int y, int z) {
        int prevX = countAtX.getOrDefault(x, 0);
        countAtX.put(x, prevX + 1);
        int prevY = countAtY.getOrDefault(y, 0);
        countAtY.put(y, prevY + 1);
        int prevZ = countAtZ.getOrDefault(z, 0);
        countAtZ.put(z, prevZ + 1);

        if (!boundsInitialized) {
            minX = maxX = x;
            minY = maxY = y;
            minZ = maxZ = z;
            boundsInitialized = true;
            updateBoundsCache();
            return;
        }

        boolean changed = false;
        if (x < minX) { minX = x; changed = true; }
        if (x > maxX) { maxX = x; changed = true; }
        if (y < minY) { minY = y; changed = true; }
        if (y > maxY) { maxY = y; changed = true; }
        if (z < minZ) { minZ = z; changed = true; }
        if (z > maxZ) { maxZ = z; changed = true; }
        
        if (changed) updateBoundsCache();
    }

    private void onRemoveCoords(int x, int y, int z) {
        int prevX = countAtX.getOrDefault(x, 0);
        int nextX = prevX - 1;
        if (nextX <= 0) countAtX.remove(x); else countAtX.put(x, nextX);
        int prevY = countAtY.getOrDefault(y, 0);
        int nextY = prevY - 1;
        if (nextY <= 0) countAtY.remove(y); else countAtY.put(y, nextY);
        int prevZ = countAtZ.getOrDefault(z, 0);
        int nextZ = prevZ - 1;
        if (nextZ <= 0) countAtZ.remove(z); else countAtZ.put(z, nextZ);

        if (!boundsInitialized) return;

        if (volume.isEmpty()) {
            countAtX.clear(); countAtY.clear(); countAtZ.clear();
            boundsInitialized = false;
            cachedBounds = new AABB(0,0,0,0,0,0);
            return;
        }

        boolean changed = false;

        if (x == minX && !countAtX.containsKey(minX)) {
            while (minX <= maxX && !countAtX.containsKey(minX)) minX++;
            changed = true;
        }
        if (x == maxX && !countAtX.containsKey(maxX)) {
            while (maxX >= minX && !countAtX.containsKey(maxX)) maxX--;
            changed = true;
        }

        if (y == minY && !countAtY.containsKey(minY)) {
            while (minY <= maxY && !countAtY.containsKey(minY)) minY++;
            changed = true;
        }
        if (y == maxY && !countAtY.containsKey(maxY)) {
            while (maxY >= minY && !countAtY.containsKey(maxY)) maxY--;
            changed = true;
        }

        if (z == minZ && !countAtZ.containsKey(minZ)) {
            while (minZ <= maxZ && !countAtZ.containsKey(minZ)) minZ++;
            changed = true;
        }
        if (z == maxZ && !countAtZ.containsKey(maxZ)) {
            while (maxZ >= minZ && !countAtZ.containsKey(maxZ)) maxZ--;
            changed = true;
        }

        if (changed) updateBoundsCache();
    }

    private void updateBoundsCache() {
        if (!boundsInitialized) {
            cachedBounds = new AABB(0,0,0,0,0,0);
            return;
        }
        cachedBounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
