package com.deltasf.createpropulsion.magnet;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector3d;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.debug.DebugRenderer;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("deprecation")
public class MagnetRegistry {
    //#region Singleton
    private static MagnetRegistry INSTANCE;
    public static MagnetRegistry get() {
        if (INSTANCE == null) INSTANCE = new MagnetRegistry();
        return INSTANCE;
    }
    private MagnetRegistry() {}
    //#endregion
    private final double magnetRange = 32.0;
    public final double magnetRangeSquared = magnetRange * magnetRange;
    public final boolean debug = false;

    //Broad check
    private final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<List<MagnetData>>> dimensionMap = new HashMap<>();
    private final Map<MagnetData, Long> lastChunkKey = new IdentityHashMap<>();

    //Pairing
    private final ConcurrentHashMap<Long, List<MagnetPair>> shipToPairs = new ConcurrentHashMap<>();
    
    public void updateMagnet(Level level, MagnetData data) {
        data.updateWorldPosition(level);
        long newChunkKey = positionToPackedChunkPos(data.getPosition());

        Long2ObjectOpenHashMap<List<MagnetData>> spatial = dimensionMap.computeIfAbsent(level.dimension(), k -> new Long2ObjectOpenHashMap<>());

        Long oldChunkKey = lastChunkKey.get(data);
        // Always remove from old position if it exists
        if (oldChunkKey != null) {
            List<MagnetData> oldList = spatial.get(oldChunkKey);
            if (oldList != null) oldList.remove(data);
            lastChunkKey.remove(data);
        }
        // Add to new position
        spatial.computeIfAbsent(newChunkKey, k -> new ArrayList<>()).add(data);
        lastChunkKey.put(data, newChunkKey);

        //TODO: force immediate update
    }

    public void removeMagnet(Level level, MagnetData data) {
        Long2ObjectOpenHashMap<List<MagnetData>> spatial = dimensionMap.get(level.dimension());
        if (spatial == null) return;
        Long oldKey = lastChunkKey.get(data);
        if (oldKey != null) {
            List<MagnetData> oldList = spatial.get(oldKey);
            if (oldList != null) oldList.remove(data);
            lastChunkKey.remove(data);
        }
    }

    //TODO: Instead of doing neighbour scan every tick - do it only when one of the magnets moved far enough (> 0.2 blocks) from previous registered position
    //However it should keep worldPosition updated, and update registered position only when moved too far away
    //When this happens - do retrieveNeighbours for this magnet only
    //This will make sure that registry is updated only when needed and will increase performance

    public List<MagnetData> retrieveNeighbours(Level level, MagnetData data) {
        Vector3d position = data.getPosition();
        int cx = Mth.floor(position.x) >> 4;
        int cz = Mth.floor(position.z) >> 4;

        Long2ObjectOpenHashMap<List<MagnetData>> spatial = dimensionMap.get(level.dimension());
        if (spatial == null) return Collections.emptyList();

        List<MagnetData> neighbours = new ArrayList<>();
        for(int dx = -2; dx <= 2; dx++) {
            for(int dz = -2; dz <= 2; dz++) {
                long key = packChunkPos(cx + dx, cz + dz);
                List<MagnetData> list = spatial.getOrDefault(key, Collections.emptyList());
                neighbours.addAll(list);
            }
        }
        return neighbours;
    }

    public void computePairs(Level level) {
        Long2ObjectOpenHashMap<List<MagnetData>> spatial = dimensionMap.get(level.dimension());
        if (spatial == null) return;
        //Gather active magnets
        List<MagnetData> active = new ArrayList<>();
        for(var list : spatial.values()) { active.addAll(list); }
        int n = active.size();
        //Debug, draw active
        if (debug) {
            for (int i = 0; i < n; i++) {
                var data = active.get(i);
                String identifier = i + "_active";
                DebugRenderer.drawBox(identifier, data.getBlockPos().getCenter(), new Vec3(1.5, 1.5, 1.5), Color.red, 2);
                Vec3 snd = data.getBlockPos().getCenter().add(new Vec3(data.getBlockDipoleDir().x, data.getBlockDipoleDir().y, data.getBlockDipoleDir().z));
                DebugRenderer.drawElongatedBox(identifier + "_dir", data.getBlockPos().getCenter(), snd, 0.1f, Color.blue, false, 2);
            }
        }
        
        if (n <= 1) {
            // Nothing to pair if there's 0 or 1 magnet
            shipToPairs.clear();
            return;
        }
        //DisjointSet uf = new DisjointSet(n);
        List<int[]> edges = new ArrayList<>();

        // Build a map from MagnetData to its index for fast lookup
        Map<MagnetData, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            indexMap.put(active.get(i), i);
        }
        shipToPairs.clear();
        //Neighbour scan
        for(int i = 0; i < n; i++) {
            MagnetData A = active.get(i);
            Vector3d posA = A.getPosition();
            //if (A.shipId == -1) continue; //No need to scan the static magnets, dynamic will find them anyway 
            //(technically there is a need due to j<= i check, but this is still possible if we restructure this)
            for(MagnetData B : retrieveNeighbours(level, A)) {
                Integer jObj = indexMap.get(B);
                if (jObj == null) continue; // B not in active list
                int j = jObj;
                if (j <= i) continue;
                if (shouldSkip(A, B)) continue;
                Vector3d posB = B.getPosition();
                if (posA.distanceSquared(posB) <= magnetRangeSquared) {
                    //uf.union(i, j);
                    edges.add(new int[]{i, j});

                }
            }
        }
        //Add pairs
        for(int[] edge : edges) {
            MagnetData A = active.get(edge[0]);
            MagnetData B = active.get(edge[1]);
            if (A.shipId != -1) {
                MagnetPair pair = new MagnetPair(A.getBlockPos(), A.getBlockDipoleDir(), B.shipId, B.getBlockPos(), B.getBlockDipoleDir());
                shipToPairs.computeIfAbsent(A.shipId, k -> new ArrayList<>()).add(pair);
            }
            if (B.shipId != -1) {
                MagnetPair pair = new MagnetPair(B.getBlockPos(), B.getBlockDipoleDir(), A.shipId, A.getBlockPos(), A.getBlockDipoleDir());
                shipToPairs.computeIfAbsent(B.shipId, k -> new ArrayList<>()).add(pair);
            }
        }

        //Debug, draw edges
        if (debug) {
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

    private boolean shouldSkip(MagnetData A, MagnetData B) {
        if (A.shipId == -1 && B.shipId == -1) return true; //Both on world grid
        if (A.shipId != -1 && A.shipId == B.shipId) return true; //Both on the same ship
        return false;
    }

    //Utility

    public void reset() {
        dimensionMap.clear();
        lastChunkKey.clear();
        shipToPairs.clear();
    }

    public List<MagnetPair> getPairsForShip(long shipId) {
        List<MagnetPair> internal = shipToPairs.get(shipId);
        if (internal == null || internal.isEmpty()) {
            return Collections.emptyList();
        }
        // Return a shallow copy so physics thread can iterate safely
        return new ArrayList<>(internal);
    }

    public long positionToPackedChunkPos(Vector3d position) {
        return packChunkPos(Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
    }

    public long packChunkPos(int chunkX, int chunkZ) {
        return (long)((chunkX & 0xFFFFFFFFL) << 32 | (chunkZ & 0xFFFFFFFFL));
    }
}
