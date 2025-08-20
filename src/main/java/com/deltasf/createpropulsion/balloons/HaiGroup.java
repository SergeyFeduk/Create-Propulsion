package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.valkyrienskies.core.impl.shadow.cl;
import org.valkyrienskies.core.impl.shadow.pu;

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

    public void addHai(HaiData data) {
        hais.add(data);
    }

    public List<BalloonRegistry.HaiData> getHais() {
        return this.hais;
    }

    public AABB getGroupAABB() {
        return groupAABB;
    }

    public List<Balloon> getFinalizedBalloons() {
        return this.finalizedBalloons;
    }


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

    public class BlobNode {
        public int id;
        public Set<BlockPos> volume = new HashSet<>();
        public Set<Integer> parentIds = new HashSet<>();
        public Set<Integer> childrenIds = new HashSet<>();
        boolean hasFatalLeak;
    }

    public class WorkItem {
        public BlockPos seed;
        public int y;
        public WorkItem(BlockPos seed) {
            this.seed = seed;
            this.y = seed.getY();
        }
    }

    private record BlobScanResult(Set<BlockPos> volume, boolean hasLeak) {}

    PriorityQueue<WorkItem> workList = new PriorityQueue<>((WorkItem a, WorkItem b) -> Integer.compare(b.y, a.y));
    Map<Integer, BlobNode> graph = new HashMap<>();
    Map<BlockPos, Integer> blockToNodeId = new HashMap<>();
    int nextNodeId = 0;
    public int getNextNodeId() { return nextNodeId++; }
    public static final int VERTICAL_PROBE_DISTANCE = 32;

    public static boolean isHab(BlockState state) {
        return state.is(PropulsionBlocks.HAB_BLOCK.get());
    }

    public List<Balloon> finalizedBalloons = new ArrayList<>();

    public void scan(Level level) {
        //Initialize scan
        graph.clear();
        blockToNodeId.clear();
        workList.clear();
        nextNodeId = 0;
        seedWorklistFromHais(level);
        //Phase 1: Scan worklist and construct a DAG
        while (!workList.isEmpty()) {
            WorkItem currentWork = workList.poll();
            if (blockToNodeId.containsKey(currentWork.seed)) continue; //Skip redundant
            scanAndProcessSeed(currentWork.seed, level);
        }
        //Phase 2: Finalize the graph
        finalizeGraph();
    }

    private void finalizeGraph() {
        Set<Integer> invalidNodeIds = new HashSet<>();
        Queue<Integer> toPrune = new LinkedList<>();

        //Find initial invalid links
        for (BlobNode node : graph.values()) {
            if (node.hasFatalLeak) { 
                invalidNodeIds.add(node.id);
                toPrune.add(node.id);
            }
        }

        //Propagate invalidation
        while (!toPrune.isEmpty()) {
            int invalidId = toPrune.poll();
            BlobNode invalidNode = graph.get(invalidId);
            for (Integer childrenId : invalidNode.childrenIds) {
                if (invalidNodeIds.contains(childrenId)) continue;
                invalidNodeIds.add(childrenId);
                toPrune.add(childrenId);
            }
        }

        //Resolve connectivity
        List<Balloon> finalBalloons = new ArrayList<>();
        Set<Integer> visitedValidNodes = new HashSet<>();

        for (BlobNode node : graph.values()) {
            if (invalidNodeIds.contains(node.id) || visitedValidNodes.contains(node.id)) continue; //Skip invalid or visited nodes

            //Create new balloon
            Balloon newBalloon = new Balloon();
            Queue<Integer> toGroup = new LinkedList<>();
            toGroup.add(node.id);
            visitedValidNodes.add(node.id);
            //Group via traversal
            while (!toGroup.isEmpty()) {
                int currentId = toGroup.poll();
                BlobNode currentNode = graph.get(currentId);

                newBalloon.interiorAir.addAll(currentNode.volume);
                for (Integer parentId : currentNode.parentIds) {
                    if (invalidNodeIds.contains(parentId) || visitedValidNodes.contains(parentId)) continue;
                    toGroup.add(parentId);
                    visitedValidNodes.add(parentId);
                }

                for (Integer childrenId : currentNode.childrenIds) {
                    if (invalidNodeIds.contains(childrenId) || visitedValidNodes.contains(childrenId)) continue;
                    toGroup.add(childrenId);
                    visitedValidNodes.add(childrenId);
                }
            }

            finalBalloons.add(newBalloon);
        }

        finalizedBalloons.clear();
        finalizedBalloons.addAll(finalBalloons);
    }

    public class Balloon {
        public Set<BlockPos> interiorAir = new HashSet<>();
        public Set<BlockPos> shell = new HashSet<>();
    }

    private void seedWorklistFromHais(Level level) {
        for(int i = 0; i < hais.size(); i++) {
            HaiData data = hais.get(i);
            for(int d = 0; d < VERTICAL_PROBE_DISTANCE; d++) {
                BlockState nextBlockState = level.getBlockState(data.position().above(d));
                if (isHab(nextBlockState)) {
                    WorkItem workItem = new WorkItem(data.position().above(d-1)); //d-1 as we need a block below the hab block
                    workList.add(workItem);
                    break;
                }
            }
        }
    }

    private void scanAndProcessSeed(BlockPos seed, Level level) {
        BlobScanResult scanResult = discoverBlob(seed, level);
        
        //Create and process the node
        BlobNode node = new BlobNode();
        node.id = getNextNodeId();
        node.volume = scanResult.volume();
        node.hasFatalLeak = scanResult.hasLeak;
        graph.put(node.id, node);

        /*for(BlockPos pos : node.volume) {
            //Populate global map
            blockToNodeId.put(pos, node.id);

            //Find parents
            BlockPos above = pos.above();
            boolean isAboveDiscovered = blockToNodeId.containsKey(above);
            if (isAboveDiscovered) {
                int parentId = blockToNodeId.get(above);
                node.parentIds.add(parentId);
                BlobNode parent = graph.get(parentId);
                parent.childrenIds.add(node.id);
            }

            //Find children (not useful for normal nodes but required for anomaly-discovered ones)
            BlockPos below = pos.below();
            if (blockToNodeId.containsKey(below)) {
                int childId = blockToNodeId.get(below);
                node.childrenIds.add(childId);
                BlobNode child = graph.get(childId);
                child.parentIds.add(node.id);
            }

            //Discover anomalies
            if (!isAboveDiscovered && !isHab(level.getBlockState(above))) {
                BlockPos anomalyStartPos = above;
                //Do a vertical probe
                int distanceToHab = -1; // -1 is a leak flag. Any other value is vertical distance to hab block
                for(int d = 0; d < VERTICAL_PROBE_DISTANCE; d++) {
                    BlockPos probePos = anomalyStartPos.above(d);
                    BlockState nextBlockState = level.getBlockState(probePos);
                    if (isHab(nextBlockState)) {
                        distanceToHab = d;
                        break;
                    }
                    if (!isInsideRleVolume(probePos)) {
                        //Found a leak due to leaving RLE volume
                        node.hasFatalLeak = true;
                        break;
                    }
                }

                if (distanceToHab == -1) {
                    //Found a leak
                    node.hasFatalLeak = true;
                } else {
                    //There is no leak, create and queue new seed
                    WorkItem workItem = new WorkItem(anomalyStartPos.above(distanceToHab-1));
                    workList.add(workItem);
                }
            }
        }

        if (!node.hasFatalLeak) {
            //Node is non-leaky guaranteed. Seed blocks below it
            for (BlockPos pos : node.volume) {
                WorkItem childWorkItem = new WorkItem(pos.below());
                workList.add(childWorkItem);
            }
        }*/

        if (!node.hasFatalLeak) {
            for (BlockPos pos : node.volume) {
                BlockPos above = pos.above();
                boolean isAboveDiscovered = blockToNodeId.containsKey(above);
                
                // Discover anomalies ONLY if the node is currently considered sealed.
                if (!isAboveDiscovered && !isHab(level.getBlockState(above))) {
                    BlockPos anomalyStartPos = above;
                    int distanceToHab = -1;
                    for (int d = 0; d < VERTICAL_PROBE_DISTANCE; d++) {
                        BlockPos probePos = anomalyStartPos.above(d);
                        BlockState nextBlockState = level.getBlockState(probePos);
                        if (isHab(nextBlockState)) {
                            distanceToHab = d;
                            break;
                        }
                        if (!isInsideRleVolume(probePos)) {
                            node.hasFatalLeak = true;
                            break;
                        }
                    }

                    if (distanceToHab == -1) {
                        node.hasFatalLeak = true;
                    } else {
                        WorkItem workItem = new WorkItem(anomalyStartPos.above(distanceToHab - 1));
                        workList.add(workItem);
                    }

                    // If an anomaly probe found a leak, we can stop checking for other anomalies.
                    if (node.hasFatalLeak) {
                        break; 
                    }
                }
            }
        }

        // 4. Now that the node's leak status is FINAL, we perform all linking and seeding.
        if (!node.hasFatalLeak) {
            // The node is sealed. It can be a valid part of the graph and can seed downwards.
            for (BlockPos pos : node.volume) {
                // Populate global map
                blockToNodeId.put(pos, node.id);

                // Find parents
                BlockPos above = pos.above();
                if (blockToNodeId.containsKey(above)) {
                    int parentId = blockToNodeId.get(above);
                    node.parentIds.add(parentId);
                    BlobNode parent = graph.get(parentId);
                    if (parent != null) parent.childrenIds.add(node.id);
                }

                // Find children
                BlockPos below = pos.below();
                if (blockToNodeId.containsKey(below)) {
                    int childId = blockToNodeId.get(below);
                    node.childrenIds.add(childId);
                    BlobNode child = graph.get(childId);
                    if (child != null) child.parentIds.add(node.id);
                }
                
                // Seed blocks below
                WorkItem childWorkItem = new WorkItem(pos.below());
                workList.add(childWorkItem);
            }
        } else {
            // The node is leaky. Its ONLY purpose is to occupy space to prevent re-scans.
            // We do NOT link it to parents/children and we do NOT seed below it.
            for (BlockPos pos : node.volume) {
                blockToNodeId.put(pos, node.id);
            }
        }
    }

    private BlobScanResult discoverBlob(BlockPos seed, Level level) {
        Set<BlockPos> volume = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        boolean hasLeak = false;

        queue.add(seed);
        volume.add(seed);

        //BFS
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = currentPos.relative(dir);

                if (!isInsideRleVolume(neighborPos)) {
                    hasLeak = true;
                    continue; //Found a leak
                }

                if (volume.contains(neighborPos) || blockToNodeId.containsKey(neighborPos)) {
                    continue; //Already visited
                }

                BlockState neighbourState = level.getBlockState(neighborPos);
                if (isHab(neighbourState)) {
                    continue; //Found a hab
                }

                volume.add(neighborPos);
                queue.add(neighborPos);
            }
        }

        return new BlobScanResult(volume, hasLeak);
    }
}
