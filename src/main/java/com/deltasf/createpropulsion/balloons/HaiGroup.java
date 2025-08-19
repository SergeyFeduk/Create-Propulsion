package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.deltasf.createpropulsion.balloons.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HaiGroup {
    private final List<HaiData> hais = new ArrayList<>();
    private List<Pair<Integer, Integer>>[][] rleVolume;
    private AABB groupAABB;

    //private final List<Balloon> finalizedBalloons = new ArrayList<>();


    /*public static class Balloon {
        public final Set<BlockPos> interiorAir = new HashSet<>();
        public final Set<BlockPos> shell = new HashSet<>();
        public final DisjointSetUnion connectivity = new DisjointSetUnion();
    }

    private static class PotentialBalloon {
        private int id;
        private final Set<BlockPos> volume = new HashSet<>();
        private final Set<BlockPos> shell = new HashSet<>(); 
        public PotentialBalloon(int id) { this.id = id; }
    }

    private record LBLSubGroup(Set<BlockPos> volume, Set<BlockPos> traversed) {}*/

    public void addHai(HaiData data) {
        hais.add(data);
    }

    public List<BalloonRegistry.HaiData> getHais() {
        return this.hais;
    }

    public AABB getGroupAABB() {
        return groupAABB;
    }

    /*public List<Balloon> getFinalizedBalloons() {
        return this.finalizedBalloons;
    }*/


    public List<Pair<Integer, Integer>>[][] getRleVolume() {
        return rleVolume;
    }

    //TODO: Optimize so it DOES NOT use the bounding AABB. 
    @SuppressWarnings("unchecked") // For the generic array creation
    public void generateRleVolume() {
        if (hais.isEmpty()) {
            return;
        }

        // 1. Calculate the single bounding box for the whole group.
        this.groupAABB = hais.get(0).maxAABB();
        for (int i = 1; i < hais.size(); i++) {
            this.groupAABB = this.groupAABB.minmax(hais.get(i).maxAABB());
        }

        int sizeX = (int) (groupAABB.maxX - groupAABB.minX);
        int sizeY = (int) (groupAABB.maxY - groupAABB.minY);
        rleVolume = new List[sizeY][sizeX]; // Initialize the 2D array

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // 2. Loop through every Y/X column in the group's AABB.
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                int worldX = (int) groupAABB.minX + x;
                int worldY = (int) groupAABB.minY + y;
                mutablePos.set(worldX, worldY, 0);

                List<Pair<Integer, Integer>> zIntervals = new ArrayList<>();

                // 3. Collect all Z-intervals from HAIs that cover this column.
                for (BalloonRegistry.HaiData hai : hais) {
                    if (hai.maxAABB().contains(mutablePos.getX(), mutablePos.getY(), hai.maxAABB().minZ)) {
                        zIntervals.add(Pair.of((int) hai.maxAABB().minZ, (int) hai.maxAABB().maxZ));
                    }
                }

                // 4. Sort and merge the intervals, then store them.
                if (!zIntervals.isEmpty()) {
                    rleVolume[y][x] = mergeIntervals(zIntervals);
                }
            }
        }
    }

    private static List<Pair<Integer, Integer>> mergeIntervals(List<Pair<Integer, Integer>> intervals) {
        if (intervals.size() <= 1) {
            return intervals;
        }

        // Sort intervals by their starting point.
        intervals.sort(Comparator.comparing(Pair::getFirst));

        LinkedList<Pair<Integer, Integer>> merged = new LinkedList<>();
        for (Pair<Integer, Integer> interval : intervals) {
            // If merged list is empty or the current interval does not overlap with the previous,
            // simply add it.
            if (merged.isEmpty() || merged.getLast().getSecond() < interval.getFirst() - 1) {
                merged.add(interval);
            }
            // Otherwise, there is an overlap, so we merge the current and previous intervals.
            else {
                Pair<Integer, Integer> last = merged.getLast();
                merged.set(merged.size() - 1, Pair.of(last.getFirst(), Math.max(last.getSecond(), interval.getSecond())));
            }
        }

        return merged;
    }

    public boolean isInsideRleVolume(BlockPos pos) {
        if (groupAABB == null) return false;
        int y = pos.getY() - (int) groupAABB.minY;
        int x = pos.getX() - (int) groupAABB.minX;

        if (y < 0 || y >= rleVolume.length || x < 0 || x >= rleVolume[y].length) {
            return false;
        }

        List<Pair<Integer, Integer>> zIntervals = rleVolume[y][x];
        if (zIntervals == null || zIntervals.isEmpty()) {
            return false;
        }

        int worldZ = pos.getZ();
        for (Pair<Integer, Integer> interval : zIntervals) {
            if (worldZ >= interval.getFirst() && worldZ <= interval.getSecond()) {
                return true;
            }
        }
        return false;
    }

    /*public void scan(Level level) {
        if (rleVolume == null || groupAABB == null) { return; }
        finalizedBalloons.clear();
        Map<Integer, PotentialBalloon> inProgressBalloons = new HashMap<>();
        int nextBalloonId = 0;
        int topY = (int) groupAABB.maxY;
        int bottomY = (int) groupAABB.minY;
        for (int y = topY; y >= bottomY; y--) {
            List<LBLSubGroup> currentLayerSubGroups = scanLayer(y, level);
            nextBalloonId = correlateLayerResults(currentLayerSubGroups, inProgressBalloons, level, nextBalloonId);
        }
        for (PotentialBalloon pb : inProgressBalloons.values()) {
            finalizeBalloon(pb);
        }
    }*/


    /*private void finalizeBalloon(PotentialBalloon pb) {
        if (pb.volume.isEmpty()) return;

        Balloon finalBalloon = new Balloon();
        finalBalloon.interiorAir.addAll(pb.volume);
        finalBalloon.shell.addAll(pb.shell);

        // ### START OF THE CRITICAL FIX ###
        // Phase 1: Initialize every single element in the DSU first.
        for (BlockPos pos : finalBalloon.interiorAir) {
            finalBalloon.connectivity.makeSet(pos.hashCode());
        }

        // Phase 2: Now that all elements are guaranteed to exist, connect them.
        for (BlockPos pos : finalBalloon.interiorAir) {
            // Only need to check in 3 directions to avoid redundant checks
            for (Direction dir : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.DOWN}) {
                BlockPos neighbor = pos.relative(dir);
                if (finalBalloon.interiorAir.contains(neighbor)) {
                    finalBalloon.connectivity.union(pos.hashCode(), neighbor.hashCode());
                }
            }
        }
        // ### END OF THE CRITICAL FIX ###

        finalizedBalloons.add(finalBalloon);
    }*/



    /*private List<LBLSubGroup> scanLayer(int y, Level level) {
        List<LBLSubGroup> foundSubGroups = new ArrayList<>();
        Set<BlockPos> visitedOnThisLayer = new HashSet<>();
        for (HaiData hai : hais) {
            BlockPos seedPos = new BlockPos(hai.position().getX(), y, hai.position().getZ());
            if (visitedOnThisLayer.contains(seedPos)) continue;
            // The floodFillLayer method will handle checking if the seed is valid to start from.
            LBLSubGroup potentialSubGroup = floodFillLayer(seedPos, level, visitedOnThisLayer);
            if (potentialSubGroup != null && !potentialSubGroup.volume().isEmpty()) {
                foundSubGroups.add(potentialSubGroup);
            }
        }
        return foundSubGroups;
    }


    public static boolean isHab(BlockState state) {
        return state.is(PropulsionBlocks.HAB_BLOCK.get());
    }*/


    /*private LBLSubGroup floodFillLayer(BlockPos start, Level level, Set<BlockPos> globalVisited) {
        if (!isInsideRleVolume(start) || globalVisited.contains(start)) { return null; }
        BlockState startState = level.getBlockState(start);
        if (isHab(startState)) { return null; }

        Set<BlockPos> foundVolume = new HashSet<>();
        Set<BlockPos> traversedInBlob = new HashSet<>();
        Queue<BlockPos> toVisit = new LinkedList<>();

        toVisit.add(start);
        globalVisited.add(start);
        traversedInBlob.add(start);

        while (!toVisit.isEmpty()) {
            BlockPos currentPos = toVisit.poll();
            if (!isHab(level.getBlockState(currentPos))) {
                foundVolume.add(currentPos);
            }
            BlockPos[] neighbors = { currentPos.north(), currentPos.south(), currentPos.east(), currentPos.west() };
            for (BlockPos neighborPos : neighbors) {
                if (globalVisited.contains(neighborPos)) continue;
                if (!isInsideRleVolume(neighborPos)) { return null; }
                globalVisited.add(neighborPos);
                BlockState neighborState = level.getBlockState(neighborPos);
                boolean isTraversable = !isHab(neighborState);
                if (isTraversable) {
                    toVisit.add(neighborPos);
                    traversedInBlob.add(neighborPos);
                }
            }
        }
        return new LBLSubGroup(foundVolume, traversedInBlob);
    }*/



    /*private Set<BlockPos> validateAndCollectShellForBlob(LBLSubGroup subGroup, Map<BlockPos, PotentialBalloon> parentLayerMap, Level level) {
        Set<BlockPos> airBlob = subGroup.volume();
        Set<BlockPos> traversedBlob = subGroup.traversed();
        Set<BlockPos> foundShell = new HashSet<>();

        for (BlockPos pos : airBlob) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);

                // If the neighbor is part of the same internal cavity (air or obstacle), it's not shell.
                if (traversedBlob.contains(neighborPos)) {
                    continue;
                }

                // If the ceiling is air from a parent balloon, it's a valid continuation, not shell.
                if (dir == Direction.UP && parentLayerMap.containsKey(neighborPos)) {
                    continue;
                }

                // At this point, the neighbor is TRULY external.
                BlockState neighborState = level.getBlockState(neighborPos);

                if (isHab(neighborState)) {
                    foundShell.add(neighborPos);
                } else { // The neighbor is AIR.
                    if (dir != Direction.DOWN) {
                        if (dir == Direction.UP) return new HashSet<>(); //Anomaly
                        return null;
                    }
                }
            }
        }
        return foundShell;
    }*/

    //1) Iterate upwards till we hit the y end
    //If we DO NOT FIND the hab block anywhere here - return null
    //If we FIND a HAB here - go to step 2

    //2) At the position below the found hab run floodFillLayer(posBelowHab, level, newGlobalVisited), this will return a lblsubgroup...
    //3) Do a simplified validation to check if this subgroup is not a hole (check if it leaks into RLE end or has no blocks above)
    //If it leaks - its a hole, return null
    //If it does not leak - go to step 4

    //4) If we did not reach the bottom layer (we know it from the pos field as it contains current position of the layer we were scanning) - go to step 2 but at layer below
    //If we reached the bottom layer - add all the LBLSubGroups we collected to the layers above us (we have already executed the correlateLayerResults...)
    //... on them, so there is no race condition. For this layer - add the layer above (we have scanned it) to the parentLayerMap and simply continue the scan.





    /* private int correlateLayerResults(List<LBLSubGroup> currentLayerSubGroups, Map<Integer, PotentialBalloon> inProgressBalloons, Level level, int nextId) {
        if (inProgressBalloons.isEmpty() && currentLayerSubGroups.isEmpty()) { return nextId; }

        Map<BlockPos, PotentialBalloon> parentLayerMap = new HashMap<>();
        for (PotentialBalloon pb : inProgressBalloons.values()) {
            for (BlockPos pos : pb.volume) {
                parentLayerMap.put(pos, pb);
            }
        }

        Map<LBLSubGroup, Set<BlockPos>> validatedShells = new HashMap<>();
        List<LBLSubGroup> validSubGroups = new ArrayList<>();
        for (LBLSubGroup subGroup : currentLayerSubGroups) {
            Set<BlockPos> shell = validateAndCollectShellForBlob(subGroup, parentLayerMap, level);
            if (shell != null) {
                if (shell.isEmpty()) {
                    //This is an anomaly, handle it
                }

                validSubGroups.add(subGroup);
                validatedShells.put(subGroup, shell);
            }
        }

        Map<LBLSubGroup, Set<PotentialBalloon>> connections = new HashMap<>();
        Set<PotentialBalloon> allConnectedParents = new HashSet<>();
        for (LBLSubGroup subGroup : validSubGroups) {
            Set<PotentialBalloon> parents = new HashSet<>();
            for (BlockPos pos : subGroup.volume()) { // Note: we use .volume() here for connection checks
                PotentialBalloon parent = parentLayerMap.get(pos.above());
                if (parent != null) { parents.add(parent); }
            }
            connections.put(subGroup, parents);
            allConnectedParents.addAll(parents);
        }

        Iterator<Map.Entry<Integer, PotentialBalloon>> iterator = inProgressBalloons.entrySet().iterator();
        while (iterator.hasNext()) {
            PotentialBalloon pb = iterator.next().getValue();
            if (!allConnectedParents.contains(pb)) {
                finalizeBalloon(pb);
                iterator.remove();
            }
        }

        DisjointSetUnion dsu = new DisjointSetUnion();
        for (PotentialBalloon pb : inProgressBalloons.values()) { dsu.makeSet(pb.id); }

        for (Set<PotentialBalloon> parents : connections.values()) {
            if (parents.size() > 1) {
                Iterator<PotentialBalloon> parentIter = parents.iterator();
                PotentialBalloon firstParent = parentIter.next();
                while (parentIter.hasNext()) { dsu.union(firstParent.id, parentIter.next().id); }
            }
        }

        Map<Integer, PotentialBalloon> nextInProgressBalloons = new HashMap<>();
        for (PotentialBalloon pb : inProgressBalloons.values()) {
            int rootId = dsu.find(pb.id);
            PotentialBalloon newPb = nextInProgressBalloons.computeIfAbsent(rootId, k -> new PotentialBalloon(k));
            newPb.volume.addAll(pb.volume);
            newPb.shell.addAll(pb.shell);
        }

        for (Map.Entry<LBLSubGroup, Set<PotentialBalloon>> entry : connections.entrySet()) {
            LBLSubGroup subGroup = entry.getKey();
            if (!entry.getValue().isEmpty()) {
                int rootId = dsu.find(entry.getValue().iterator().next().id);
                if (nextInProgressBalloons.containsKey(rootId)) {
                    PotentialBalloon target = nextInProgressBalloons.get(rootId);
                    target.volume.addAll(subGroup.volume());
                    target.shell.addAll(validatedShells.get(subGroup));
                }
            }
        }

        for (LBLSubGroup subGroup : validSubGroups) {
            if (connections.get(subGroup).isEmpty()) {
                PotentialBalloon newBalloon = new PotentialBalloon(nextId);
                newBalloon.volume.addAll(subGroup.volume());
                newBalloon.shell.addAll(validatedShells.get(subGroup));
                nextInProgressBalloons.put(nextId, newBalloon);
                nextId++;
            }
        }

        inProgressBalloons.clear();
        inProgressBalloons.putAll(nextInProgressBalloons);
        return nextId;
    }*/


    
}
