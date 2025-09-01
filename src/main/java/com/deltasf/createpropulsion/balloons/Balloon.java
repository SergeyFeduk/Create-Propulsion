package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class Balloon implements Iterable<BlockPos> {
    //The volume of the balloon
    private final LongOpenHashSet volume = new LongOpenHashSet();
    //Coordinate counters: number of blocks at a given X / Y / Z coordinate
    private final Int2IntMap countAtX = new Int2IntOpenHashMap();
    private final Int2IntMap countAtY = new Int2IntOpenHashMap();
    private final Int2IntMap countAtZ = new Int2IntOpenHashMap();
    //Current bounds
    private int minX, maxX, minY, maxY, minZ, maxZ;
    private boolean boundsInitialized = false;
    private AABB boundsCache = null;

    public Set<UUID> supportHais;
    public Set<BlockPos> holes = new HashSet<>();

    public Balloon(Collection<BlockPos> initialVolume, AABB initialBounds, Set<UUID> supportHais) {
        addAll(initialVolume);
        this.boundsCache = initialBounds;
        this.supportHais = supportHais;
    }

    //Api

    public Iterable<BlockPos> getVolume() {
        return this;
    }

    public AABB getAABB() {
        if (boundsCache == null) updateBoundsCache();
        return boundsCache;
    }

    public boolean isEmpty() { return volume.isEmpty(); }

    public int size() { return volume.size(); }

    public boolean contains(BlockPos pos) {
        return volume.contains(packPos(pos));
    }

    public boolean add(BlockPos pos) {
        long p = packPos(pos);
        if (volume.add(p)) {
            onAddCoords(pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        return false;
    }

    public boolean remove(BlockPos pos) {
        long p = packPos(pos);
        if (volume.remove(p)) {
            onRemoveCoords(pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        return false;
    }

    public void addAll(Collection<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) return;
        for (BlockPos p : positions) volume.add(packPos(p));
    }

    public void mergeFrom(Balloon other) {
        if (other == null) return;
        final LongIterator it = other.volume.iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            if (volume.add(packed)) {
                int x = unpackX(packed), y = unpackY(packed), z = unpackZ(packed);
                onAddCoords(x, y, z);
            }
        }
    }

    public void addAllTo(Collection<BlockPos> out) {
        Objects.requireNonNull(out);
        final LongIterator it = volume.iterator();
        while (it.hasNext()) out.add(unpackPos(it.nextLong()));
    }

    public List<BlockPos> toList() {
        List<BlockPos> list = new ArrayList<>(volume.size());
        addAllTo(list);
        return list;
    }

    @Override
    public Iterator<BlockPos> iterator() {
        final LongIterator it = volume.iterator();
        return new Iterator<BlockPos>() {
            @Override
            public boolean hasNext() { return it.hasNext(); }
            @Override
            public BlockPos next() { return unpackPos(it.nextLong()); }
        };
    }

    //Incremental bounds

    private void onAddCoords(int x, int y, int z) {
        // increment X counter
        int prevX = countAtX.getOrDefault(x, 0);
        countAtX.put(x, prevX + 1);

        // increment Y counter
        int prevY = countAtY.getOrDefault(y, 0);
        countAtY.put(y, prevY + 1);

        // increment Z counter
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
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
        if (z < minZ) minZ = z;
        if (z > maxZ) maxZ = z;
        updateBoundsCache();
    }

    private void onRemoveCoords(int x, int y, int z) {
        // decrement X counter
        int prevX = countAtX.getOrDefault(x, 0);
        int nextX = prevX - 1;
        if (nextX <= 0) countAtX.remove(x); else countAtX.put(x, nextX);

        // decrement Y counter
        int prevY = countAtY.getOrDefault(y, 0);
        int nextY = prevY - 1;
        if (nextY <= 0) countAtY.remove(y); else countAtY.put(y, nextY);

        // decrement Z counter
        int prevZ = countAtZ.getOrDefault(z, 0);
        int nextZ = prevZ - 1;
        if (nextZ <= 0) countAtZ.remove(z); else countAtZ.put(z, nextZ);

        if (!boundsInitialized) return;

        if (volume.isEmpty()) {
            countAtX.clear(); countAtY.clear(); countAtZ.clear();
            boundsInitialized = false;
            boundsCache = new AABB(0,0,0,0,0,0);
            return;
        }

        if (x == minX && !countAtX.containsKey(minX)) {
            while (minX <= maxX && !countAtX.containsKey(minX)) minX++;
        }
        if (x == maxX && !countAtX.containsKey(maxX)) {
            while (maxX >= minX && !countAtX.containsKey(maxX)) maxX--;
        }

        if (y == minY && !countAtY.containsKey(minY)) {
            while (minY <= maxY && !countAtY.containsKey(minY)) minY++;
        }
        if (y == maxY && !countAtY.containsKey(maxY)) {
            while (maxY >= minY && !countAtY.containsKey(maxY)) maxY--;
        }

        if (z == minZ && !countAtZ.containsKey(minZ)) {
            while (minZ <= maxZ && !countAtZ.containsKey(minZ)) minZ++;
        }
        if (z == maxZ && !countAtZ.containsKey(maxZ)) {
            while (maxZ >= minZ && !countAtZ.containsKey(maxZ)) maxZ--;
        }

        updateBoundsCache();
    }

    private void updateBoundsCache() {
    if (!boundsInitialized) {
            boundsCache = new AABB(0,0,0,0,0,0);
            return;
        }
        boundsCache = new AABB((double) minX,     (double) minY,     (double) minZ,
                               (double) maxX + 1, (double) maxY + 1, (double) maxZ + 1);
    }

    //Position packing

    private static long packPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    private static int unpackX(long packed) {
        int x = (int) (packed >> 38);
        if (x >= (1 << 25)) x -= (1 << 26);
        return x;
    }

    private static int unpackY(long packed) {
        int y = (int) (packed & 0xFFFL);
        if (y >= (1 << 11)) y -= (1 << 12);
        return y;
    }

    private static int unpackZ(long packed) {
        int z = (int) ((packed >> 12) & 0x3FFFFFFL);
        if (z >= (1 << 25)) z -= (1 << 26);
        return z;
    }

    private static BlockPos unpackPos(long packed) {
        return new BlockPos(unpackX(packed), unpackY(packed), unpackZ(packed));
    }

    private static long packPos(BlockPos pos) {
        return packPos(pos.getX(), pos.getY(), pos.getZ());
    }
}
