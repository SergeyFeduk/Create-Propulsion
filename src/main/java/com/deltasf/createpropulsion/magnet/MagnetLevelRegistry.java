package com.deltasf.createpropulsion.magnet;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.joml.Vector3d;
import org.joml.Vector3i;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.debug.DebugRenderer;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("deprecation")
public class MagnetLevelRegistry {

    private final ConcurrentHashMap<UUID, MagnetData> magnets = new ConcurrentHashMap<>();
    private final Long2ObjectOpenHashMap<List<UUID>> spatial = new Long2ObjectOpenHashMap<>();
    private final Map<UUID, Long> lastChunkKey = new ConcurrentHashMap<>();
    private volatile Map<Long, List<MagnetPair>> shipToPairs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<UUID>> shipMagnets = new ConcurrentHashMap<>();
    
    private Level level;

    public MagnetLevelRegistry(Level level) {
        this.level = level;
        System.out.println("Creating MagnetLevelRegistry for " + level.dimension().toString());
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
        if (data != null) {
            data.scheduleForRemoval();
        }
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

    //TODO: Instead of doing neighbour scan every tick - do it only when one of the magnets moved far enough (> 0.2 blocks) from previous registered position
    //However it should keep worldPosition updated, and update registered position only when moved too far away
    //When this happens - do retrieveNeighbours for this magnet only
    //This will make sure that registry is updated only when needed and will increase performance

    public void computePairs() {
        //Collect garbage
        List<UUID> toRemove = new ArrayList<>();
        for (MagnetData data : magnets.values()) {
            if (data.isPendingRemoval()) {
                toRemove.add(data.id);
            }
        }

        for (UUID id : toRemove) {
            MagnetData removedData = magnets.remove(id);
            if (removedData != null) {
                Long chunkKey = lastChunkKey.remove(id);
                if (chunkKey != null) {
                    List<UUID> list = spatial.get(chunkKey);
                    if (list != null) list.remove(id);
                }
                //Remove from shipMagnets map
                if (removedData.shipId != -1) {
                    List<UUID> shipList = shipMagnets.get(removedData.shipId);
                    if (shipList != null) {
                        shipList.remove(id);
                        if (shipList.isEmpty()) {
                            shipMagnets.remove(removedData.shipId);
                        }
                    }
                }
            }
        }

        //Gather active magnets
        List<MagnetData> active = new ArrayList<>(magnets.values());
        int n = active.size();
        //Debug, draw active
        if (MagnetRegistry.get().debug) {
            for (int i = 0; i < n; i++) {
                var data = active.get(i);
                String identifier = i + "_active";
                DebugRenderer.drawBox(identifier, data.getBlockPos().getCenter(), new Vec3(1.5, 1.5, 1.5), Color.red, 2);
            }
        }
        
        if (n <= 1) {
            // Nothing to pair if there's 0 or 1 magnet
            if (!shipToPairs.isEmpty()) {
                this.shipToPairs = new ConcurrentHashMap<>();
            }
            return;
    
        }
        List<int[]> edges = new ArrayList<>();

        // Build a map from MagnetData to its index for fast lookup
        Map<MagnetData, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            indexMap.put(active.get(i), i);
        }
        
        //Neighbour scan
        for(int i = 0; i < n; i++) {
            MagnetData A = active.get(i);
            Vector3d posA = A.getPosition();
            for(UUID BUUID : retrieveNeighbours(level, A)) {
                MagnetData B = magnets.get(BUUID);
                Integer jObj = indexMap.get(B);
                if (jObj == null) continue; // B not in active list
                int j = jObj;
                if (j <= i) continue;
                if (shouldSkip(A, B)) continue;
                Vector3d posB = B.getPosition();
                if (posA.distanceSquared(posB) <= MagnetRegistry.magnetRangeSquared) {
                    edges.add(new int[]{i, j});
                }
            }
        }
        //Add pairs
        ConcurrentHashMap<Long, List<MagnetPair>> newShipToPairs = new ConcurrentHashMap<>();

        for(int[] edge : edges) {
            MagnetData A = active.get(edge[0]);
            MagnetData B = active.get(edge[1]);
            if (A.shipId != -1) {
                MagnetPair pair = new MagnetPair(A.getBlockPos(), A.getBlockDipoleDir(), A.getPower(),
                                                 B.shipId, B.getBlockPos(), B.getBlockDipoleDir(), B.getPower());
                newShipToPairs.computeIfAbsent(A.shipId, k -> new CopyOnWriteArrayList<>()).add(pair);
            }
            if (B.shipId != -1) {
                MagnetPair pair = new MagnetPair(B.getBlockPos(), B.getBlockDipoleDir(), B.getPower(), 
                                                 A.shipId, A.getBlockPos(), A.getBlockDipoleDir(), A.getPower());
                newShipToPairs.computeIfAbsent(B.shipId, k -> new CopyOnWriteArrayList<>()).add(pair);
            }
        }
        //Update shipToPairs
        this.shipToPairs = newShipToPairs;

        //Debug, draw edges
        if (MagnetRegistry.get().debug) {
            int i = 0;
            for(int[] edge : edges) {
                i++;
                String identifier = i + "_edge";
                MagnetData A = active.get(edge[0]);
                MagnetData B = active.get(edge[1]);
                
                DebugRenderer.drawElongatedBox(identifier, 
                    VectorConversionsMCKt.toMinecraft(A.getPosition()), 
                    VectorConversionsMCKt.toMinecraft(B.getPosition()), 
                    0.25f, Color.blue, false, 2);
            }
        }
    }

    public List<UUID> retrieveNeighbours(Level level, MagnetData data) {
        Vector3d position = data.getPosition();
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

    //Utility

    public List<MagnetPair> getPairsForShip(long shipId) {
        List<MagnetPair> internal = shipToPairs.get(shipId);
        if (internal == null || internal.isEmpty()) {
            return Collections.emptyList();
        }
        return internal;
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
