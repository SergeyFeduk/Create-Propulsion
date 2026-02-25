package com.deltasf.createpropulsion.magnet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.joml.Vector3d;
import org.joml.Vector3i;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.debug.routes.MainDebugRoute;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("deprecation")
public class MagnetLevelRegistry {

    public static final class PairKey {
        public final UUID local;
        public final UUID other;
        public PairKey(UUID local, UUID other) { this.local = local; this.other = other; }
        @Override public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PairKey key = (PairKey) obj;
            return local.equals(key.local) && other.equals(key.other);
        }
        @Override public int hashCode() { return 31 * local.hashCode() + other.hashCode(); }
    }

    private final ConcurrentHashMap<UUID, MagnetData> magnets = new ConcurrentHashMap<>();
    private final Long2ObjectOpenHashMap<List<UUID>> spatial = new Long2ObjectOpenHashMap<>();
    private final Map<UUID, Long> lastChunkKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<UUID>> shipMagnets = new ConcurrentHashMap<>();
    
    private final Map<Long, ConcurrentHashMap<PairKey, MagnetPair>> shipToPairs = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> activeEdges = new ConcurrentHashMap<>();
    
    private Level level;

    public MagnetLevelRegistry(Level level) {
        this.level = level;
    }

    public MagnetData getMagnet(UUID id) {
        return magnets.get(id);
    }

    public MagnetData getOrCreateMagnet(UUID id, BlockPos pos, long shipId, Vector3i dir, int power) {
        MagnetData data = magnets.computeIfAbsent(id, k -> {
            MagnetData newData = new MagnetData(id, pos, shipId, dir, power);
            shipMagnets.computeIfAbsent(shipId, sId -> new CopyOnWriteArrayList<>()).add(id);
            return newData;
        });
        return data;
    }

    public void scheduleRemoval(UUID id) {
        MagnetData data = magnets.get(id);
        if (data != null) data.scheduleForRemoval();
    }

    public void updateMagnetPosition(MagnetData data) {
        data.updateWorldPosition(level);

        long newChunkKey = positionToPackedChunkPos(data.getPosition());
        Long oldChunkKey = lastChunkKey.get(data.id);
        if (oldChunkKey != null && oldChunkKey.longValue() == newChunkKey) return;

        if (oldChunkKey != null) {
            List<UUID> oldList = spatial.get(oldChunkKey);
            if (oldList != null) oldList.remove(data.id);
        }

        spatial.computeIfAbsent(newChunkKey, k -> new CopyOnWriteArrayList<>()).add(data.id);
        lastChunkKey.put(data.id, newChunkKey);
    }

    public void computePairs() {
        List<UUID> toRemove = new ArrayList<>();
        List<MagnetData> dirty = new ArrayList<>();

        //Collect garbage and dirty magnets
        for (MagnetData data : magnets.values()) {
            if (data.isPendingRemoval()) {
                toRemove.add(data.id);
            } else if (data.needsRepairing) {
                dirty.add(data);
            }
        }

        //Sleep
        if (toRemove.isEmpty() && dirty.isEmpty()) return;

        //Handle removals
        for (UUID id : toRemove) {
            MagnetData removedData = magnets.remove(id);
            if (removedData != null) {
                Long chunkKey = lastChunkKey.remove(id);
                if (chunkKey != null) {
                    List<UUID> list = spatial.get(chunkKey);
                    if (list != null) list.remove(id);
                }
                if (removedData.shipId != -1) {
                    List<UUID> shipList = shipMagnets.get(removedData.shipId);
                    if (shipList != null) {
                        shipList.remove(id);
                        if (shipList.isEmpty()) {
                            shipMagnets.remove(removedData.shipId);
                            shipToPairs.remove(removedData.shipId);
                        }
                    }
                }
                severAllConnections(id, removedData.shipId);
            }
        }

        //Handle incremental updates
        for (MagnetData A : dirty) {
            A.needsRepairing = false;

            //Before scanning for new connections - kill all previous ones
            severAllConnections(A.id, A.shipId);

            Vector3d posA = A.getPosition();
            if (posA == null) continue;

            for (UUID BUUID : retrieveNeighbours(level, A)) {
                if (A.id.equals(BUUID)) continue;
                MagnetData B = magnets.get(BUUID);
                if (B == null || B.isPendingRemoval()) continue;
                if (shouldSkip(A, B)) continue;

                Vector3d posB = B.getPosition();
                if (posB != null && posA.distanceSquared(posB) <= MagnetRegistry.magnetRangeSquared) {
                    createConnection(A, B);
                }
            }
        }

        //Debug Rendering
        if (PropulsionDebug.isDebug(MainDebugRoute.MAGNET)) {
            int i = 0;
            for (MagnetData data : magnets.values()) {
                i++;
                DebugRenderer.drawBox(i + "_active", data.getBlockPos().getCenter(), new Vec3(1.5, 1.5, 1.5), Color.red, 2);
            }
            int j = 0;
            for (Map.Entry<UUID, Set<UUID>> entry : activeEdges.entrySet()) {
                UUID idA = entry.getKey();
                MagnetData A = magnets.get(idA);
                if (A == null) continue;
                for (UUID idB : entry.getValue()) {
                    if (idA.compareTo(idB) < 0) { //Only draw each A-B link once
                        MagnetData B = magnets.get(idB);
                        if (B == null) continue;
                        j++;
                        DebugRenderer.drawElongatedBox(j + "_edge", 
                            VectorConversionsMCKt.toMinecraft(A.getPosition()), 
                            VectorConversionsMCKt.toMinecraft(B.getPosition()), 
                            0.25f, Color.blue, false, 2);
                    }
                }
            }
        }
    }

    private void severAllConnections(UUID magnetId, long shipId) {
        Set<UUID> pairedWith = activeEdges.remove(magnetId);
        if (pairedWith == null) return;

        for (UUID otherId : pairedWith) {
            Set<UUID> otherEdges = activeEdges.get(otherId);
            if (otherEdges != null) otherEdges.remove(magnetId);

            MagnetData B = magnets.get(otherId);
            long otherShipId = B != null ? B.shipId : -1;

            if (shipId != -1) {
                ConcurrentHashMap<PairKey, MagnetPair> mapA = shipToPairs.get(shipId);
                if (mapA != null) mapA.remove(new PairKey(magnetId, otherId));
            }
            if (otherShipId != -1) {
                ConcurrentHashMap<PairKey, MagnetPair> mapB = shipToPairs.get(otherShipId);
                if (mapB != null) mapB.remove(new PairKey(otherId, magnetId));
            }
        }
    }

    private void createConnection(MagnetData A, MagnetData B) {
        Set<UUID> edgesA = activeEdges.computeIfAbsent(A.id, k -> ConcurrentHashMap.newKeySet());
        if (!edgesA.add(B.id)) return; //Prevent duplicate pairs if A and B both moved on the same tick

        Set<UUID> edgesB = activeEdges.computeIfAbsent(B.id, k -> ConcurrentHashMap.newKeySet());
        edgesB.add(A.id);

        if (A.shipId != -1) {
            MagnetPair pairA = new MagnetPair(A.id, A.getBlockPos(), A.getBlockDipoleDir(), A.getPower(),
                                              B.id, B.shipId, B.getBlockPos(), B.getBlockDipoleDir(), B.getPower());
            shipToPairs.computeIfAbsent(A.shipId, k -> new ConcurrentHashMap<>()).put(new PairKey(A.id, B.id), pairA);
        }
        if (B.shipId != -1) {
            MagnetPair pairB = new MagnetPair(B.id, B.getBlockPos(), B.getBlockDipoleDir(), B.getPower(), 
                                              A.id, A.shipId, A.getBlockPos(), A.getBlockDipoleDir(), A.getPower());
            shipToPairs.computeIfAbsent(B.shipId, k -> new ConcurrentHashMap<>()).put(new PairKey(B.id, A.id), pairB);
        }
    }

    public List<UUID> retrieveNeighbours(Level level, MagnetData data) {
        Vector3d position = data.getPosition();
        if (position == null) return Collections.emptyList();
        int cx = Mth.floor(position.x) >> 4;
        int cz = Mth.floor(position.z) >> 4;

        List<UUID> neighbours = new ArrayList<>(64);
        for(int dx = -2; dx <= 2; dx++) {
            for(int dz = -2; dz <= 2; dz++) {
                long key = packChunkPos(cx + dx, cz + dz);
                List<UUID> list = spatial.getOrDefault(key, Collections.emptyList());
                neighbours.addAll(list);
            }
        }
        return neighbours;
    }

    public Collection<MagnetPair> getPairsForShip(long shipId) {
        ConcurrentHashMap<PairKey, MagnetPair> internal = shipToPairs.get(shipId);
        if (internal == null || internal.isEmpty()) {
            return Collections.emptyList();
        }
        return internal.values();
    }

    public void removeAllMagnetsForShip(long shipId) {
        List<UUID> magnetsToRemove = shipMagnets.get(shipId);
        if (magnetsToRemove != null && !magnetsToRemove.isEmpty()) {
            for (UUID magnetId : new ArrayList<>(magnetsToRemove)) {
                scheduleRemoval(magnetId);
            }
            shipMagnets.remove(shipId);
        }
    }

    private boolean shouldSkip(MagnetData A, MagnetData B) {
        if (A.shipId == -1 && B.shipId == -1) return true; //Both on world grid
        if (A.shipId != -1 && A.shipId == B.shipId) return true; //Both on the same ship
        return false;
    }

    public long positionToPackedChunkPos(Vector3d position) {
        return packChunkPos(Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
    }

    public long packChunkPos(int chunkX, int chunkZ) {
        return (long)((chunkX & 0xFFFFFFFFL) << 32 | (chunkZ & 0xFFFFFFFFL));
    }
}
