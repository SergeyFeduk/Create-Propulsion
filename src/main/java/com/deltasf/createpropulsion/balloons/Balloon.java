package com.deltasf.createpropulsion.balloons;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class Balloon implements Iterable<BlockPos> {
    public static final int CHUNK_SIZE = 3;
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
    //Force chunks
    private final ConcurrentHashMap<ChunkKey, BalloonForceChunk> chunkMap = new ConcurrentHashMap<>();
    private final Set<ChunkKey> dirtyChunks = new HashSet<>();
    //Validation data
    private Set<UUID> supportHais;
    private Set<BlockPos> holes = new HashSet<>();
    //Gameplay data
    public volatile double hotAir = 0;
    public boolean isInvalid;

    public final int id;
    //Transient data
    private final LongOpenHashSet transientAddedBlocks = new LongOpenHashSet();
    private final LongOpenHashSet transientRemovedBlocks = new LongOpenHashSet();
    private final LongOpenHashSet transientAddedHoles = new LongOpenHashSet();
    private final LongOpenHashSet transientRemovedHoles = new LongOpenHashSet();
    private final Object dirtyLock = new Object();

    public Balloon(int id, Collection<BlockPos> initialVolume, AABB initialBounds) {
        this.id = id;
        //We are supposed to assign ManagedHaiSet externally
        this.supportHais = new HashSet<>(); 

        if (initialVolume != null && !initialVolume.isEmpty()) {
            addAll(initialVolume);
            resolveDirtyChunks();
        } else {
            if (initialBounds != null) this.boundsCache = initialBounds;
        }
        isInvalid = initialVolume == null || initialVolume.isEmpty();
    }

    public Balloon(int id, double hotAir, Set<BlockPos> holes, long[] unpackedVolume) {
        this.id = id;
        this.hotAir = hotAir;
        this.holes = holes;
        this.supportHais = new HashSet<>(); //This will be populated after relinking

        for(long pos : unpackedVolume) {
            this.add(BlockPos.of(pos));
        }

        rebuildAllCaches();
    }

    //Api

    public Iterable<BlockPos> getVolume() {
        return this;
    }

    public LongOpenHashSet getVolumeForSerialization() {
    return volume;        
    }

    public double getVolumeSize() {
        return (double)volume.size();
    }

    public AABB getAABB() {
        if (boundsCache == null) updateBoundsCache();
        return boundsCache;
    }

    public ConcurrentHashMap<ChunkKey, BalloonForceChunk> getChunkMap() {
        return chunkMap;
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
            markChunkDirtyForPos(pos.getX(), pos.getY(), pos.getZ());

            //Track transient
            synchronized(dirtyLock) {
                if (!transientRemovedBlocks.remove(p)) {
                    transientAddedBlocks.add(p);
                }
            }
            return true;
        }
        return false;
    }

    public boolean remove(BlockPos pos) {
        long p = packPos(pos);
        if (volume.remove(p)) {
            onRemoveCoords(pos.getX(), pos.getY(), pos.getZ());
            markChunkDirtyForPos(pos.getX(), pos.getY(), pos.getZ());

            //Track transient
            synchronized(dirtyLock) {
                if (!transientAddedBlocks.remove(p)) {
                    transientRemovedBlocks.add(p);
                }
            }
            return true;
        }
        return false;
    }

    public void addAll(Collection<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) return;
        for (BlockPos p : positions) {
            add(p);
        }
    }

    public void addHole(BlockPos pos) {
        if (holes.add(pos)) {
            long p = pos.asLong();
            synchronized(dirtyLock) {
                if (!transientRemovedHoles.remove(p)) {
                    transientAddedHoles.add(p);
                }
            }
        }
    }

    public void removeHole(BlockPos pos) {
        if (holes.remove(pos)) {
            long p = pos.asLong();
            synchronized(dirtyLock) {
                if (!transientAddedHoles.remove(p)) {
                    transientRemovedHoles.add(p);
                }
            }
        }
    }

    public boolean containsHoleAt(BlockPos checkPos) {
        return holes.contains(checkPos);
    }

    public Set<BlockPos> getHoles() {
        return holes;
    }

    public void mergeFrom(Balloon other) {
        if (other == null) return;
        
        // Merge the volume and mark related chunks
        final LongIterator it = other.volume.iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            if (volume.add(packed)) {
                int x = unpackX(packed), y = unpackY(packed), z = unpackZ(packed);
                onAddCoords(x, y, z);
                markChunkDirtyForPos(x, y, z);
                
                synchronized(dirtyLock) {
                    if (!transientRemovedBlocks.remove(packed)) {
                        transientAddedBlocks.add(packed);
                    }
                }
            }
        }

        for (BlockPos hole : other.holes) {
            addHole(hole);
        }

        // Migrate support hais
        supportHais.addAll(other.supportHais);
        // Migrate hot air
        hotAir += other.hotAir;
    }


    public void resolveHolesAfterMerge() {
        // Check if any holes ended up in the new volume. If they did - remove them
        List<BlockPos> holesToKill = new ArrayList<>();
        
        for(BlockPos hole : holes) {
            if (volume.contains(hole.asLong())) {
                holesToKill.add(hole);
            }
        }
        
        // Kill
        for(BlockPos hole : holesToKill) {
            removeHole(hole);
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

    //Support hais api

    public boolean isSupportHaisEmpty() {
        return supportHais.isEmpty();
    }

    public void clearSupportHais() {
        supportHais.clear();
    }

    public void setSupportHais(Set<UUID> managedSet) {
        supportHais = managedSet;
    }

    public Set<UUID> copySupportHais() {
        return new HashSet<>(supportHais);
    }

    public void addAllToSupportHais(Set<UUID> toAdd) {
        supportHais.addAll(toAdd);
    }

    public void addToSupportHais(UUID toAdd) {
        supportHais.add(toAdd);
    }

    public void removeAllFromSupportHais(Collection<UUID> toRemove) {
        supportHais.removeAll(toRemove);
    }

    public void removeFromSupportHais(UUID toRemove) {
        supportHais.remove(toRemove);
    }

    public Iterable<UUID> getSupportHais() {
        return supportHais;
    }

    public Set<UUID> getSupportHaisSet() {
        return supportHais;
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

    // Force chunks

    public void resolveDirtyChunks() {
        if (dirtyChunks.isEmpty()) return;

        List<ChunkKey> keys = new ArrayList<>(dirtyChunks);
        for (ChunkKey ck : keys) {
            int cx = ck.x();
            int cy = ck.y();
            int cz = ck.z();

            int originX = cx * CHUNK_SIZE;
            int originY = cy * CHUNK_SIZE;
            int originZ = cz * CHUNK_SIZE;
            int centerX = originX + (CHUNK_SIZE - 1) / 2;
            int centerY = originY + (CHUNK_SIZE - 1) / 2;
            int centerZ = originZ + (CHUNK_SIZE - 1) / 2;

            int blockCount = 0;
            double sumX = 0.0, sumY = 0.0, sumZ = 0.0;

            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                for (int ly = 0; ly < CHUNK_SIZE; ly++) {
                    for (int lz = 0; lz < CHUNK_SIZE; lz++) {
                        int wx = originX + lx;
                        int wy = originY + ly;
                        int wz = originZ + lz;
                        long packed = packPos(wx, wy, wz);
                        if (volume.contains(packed)) {
                            blockCount++;
                            sumX += (wx + 0.5) - (double) centerX;
                            sumY += (wy + 0.5) - (double) centerY;
                            sumZ += (wz + 0.5) - (double) centerZ;
                        }
                    }
                }
            }

            if (blockCount == 0) {
                chunkMap.remove(ck);
            } else {
                float centroidRelX = (float) (sumX / (double) blockCount);
                float centroidRelY = (float) (sumY / (double) blockCount);
                float centroidRelZ = (float) (sumZ / (double) blockCount);

                BalloonForceChunk c = new BalloonForceChunk(blockCount, centroidRelX, centroidRelY, centroidRelZ);
                chunkMap.put(ck, c);
            }
        }

        dirtyChunks.removeAll(keys);
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

    // Chunk position handling

    private static int worldToChunkCoord(int worldCoord) {
        return Math.floorDiv(worldCoord, CHUNK_SIZE);
    }

    private void markChunkDirtyForPos(int worldX, int worldY, int worldZ) {
        int cx = worldToChunkCoord(worldX);
        int cy = worldToChunkCoord(worldY);
        int cz = worldToChunkCoord(worldZ);
        dirtyChunks.add(new ChunkKey(cx, cy, cz));
    }

    //Data
    public record ChunkKey(int x, int y, int z) {}

    //Serialization

    public void writeMetadata(DataOutputStream out, BalloonRegistry registry) throws IOException {
        out.writeDouble(hotAir);
        out.writeInt(holes.size());
        out.writeInt(supportHais.size());

        //Holes
        for(BlockPos hole : holes) {
            out.writeLong(hole.asLong());
        }

        //Support hais
        if (registry != null) {
            for(UUID id : supportHais) {
                BalloonRegistry.HaiData data = registry.getHaiById(id);
                if (data != null) {
                    out.writeLong(data.position().asLong());
                }
            }
        }
    }

    public void rebuildAllCaches() {
        countAtX.clear();
        countAtY.clear();
        countAtZ.clear();
        boundsInitialized = false;

        LongIterator it = volume.iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            int uX = unpackX(packed);
            int uY = unpackY(packed);
            int uZ = unpackZ(packed);
            onAddCoords(uX, uY, uZ);
            markChunkDirtyForPos(uX, uY, uZ);
        }

        resolveDirtyChunks();
    }

    //Delta delta delta

    public DeltaData popDeltas() {
        synchronized(dirtyLock) {
            if (transientAddedBlocks.isEmpty() && transientRemovedBlocks.isEmpty() &&
                transientAddedHoles.isEmpty() && transientRemovedHoles.isEmpty()) {
                return null;
            }

            DeltaData data = new DeltaData(
                transientAddedBlocks.clone(),
                transientRemovedBlocks.clone(),
                transientAddedHoles.clone(),
                transientRemovedHoles.clone()
            );

            transientAddedBlocks.clear();
            transientRemovedBlocks.clear();
            transientAddedHoles.clear();
            transientRemovedHoles.clear();
            return data;
        }
    }

    public record DeltaData(
        LongOpenHashSet addedBlocks, LongOpenHashSet removedBlocks,
        LongOpenHashSet addedHoles, LongOpenHashSet removedHoles
    ) {}
}
