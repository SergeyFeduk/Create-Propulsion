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

import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HaiGroup {
    private final List<HaiData> hais = new ArrayList<>();
    private RLEVolume rleVolume = new RLEVolume();

    public void addHai(HaiData data) {
        hais.add(data);
    }

    public void addAllHais(List<HaiData> newHais) {
        this.hais.addAll(newHais);
    }

    public List<BalloonRegistry.HaiData> getHais() {
        return this.hais;
    }

    public void invalidateBalloonState() {
        this.finalizedBalloons.clear();
        // In the future, you could also clear other state here if needed.
    }

    public List<Balloon> getFinalizedBalloons() {
        return this.finalizedBalloons;
    }

    public RLEVolume getRleVolume() {
        return rleVolume;
    }

    public void generateRLEVolume() {
        rleVolume.setHais(hais);
        rleVolume.generateRleVolume();
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
    Set<BlockPos> initialRootSeeds = new HashSet<>();
    Map<Integer, Set<BlockPos>> anomalySources = new HashMap<>();
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
        initialRootSeeds.clear();
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

        //Find all nodes that cannot be reached from any seed and kill them
        Set<Integer> rootNodeIds = new HashSet<>();
        for (BlockPos seed : initialRootSeeds) {
            if (blockToNodeId.containsKey(seed)) {
                rootNodeIds.add(blockToNodeId.get(seed));
            }
        }

        Set<Integer> connectedToRoot = new HashSet<>();
        Queue<Integer> toVisit = new LinkedList<>(rootNodeIds);
        connectedToRoot.addAll(rootNodeIds);

        while (!toVisit.isEmpty()) {
            BlobNode currentNode = graph.get(toVisit.poll());
            if (currentNode == null) continue;
            
            // Traverse all physical parent/child connections
            for (int parentId : currentNode.parentIds) {
                if (connectedToRoot.add(parentId)) toVisit.add(parentId);
            }
            for (int childId : currentNode.childrenIds) {
                if (connectedToRoot.add(childId)) toVisit.add(childId);
            }
        }

        for (Integer nodeId : graph.keySet()) {
            if (!connectedToRoot.contains(nodeId)) {
                invalidNodeIds.add(nodeId); // Mark disconnected nodes as invalid
            }
        }

        //Propagate invalidation from anomaly bridges
        
        Map<Integer, Set<Integer>> reverseAnomalyLinks = new HashMap<>();
        for (Map.Entry<Integer, Set<BlockPos>> entry : anomalySources.entrySet()) {
            int sourceNodeId = entry.getKey();
            Set<BlockPos> discoveredSeeds = entry.getValue();

            for (BlockPos seed : discoveredSeeds) {
                Integer discoveredNodeId = blockToNodeId.get(seed);
                if (discoveredNodeId != null) {
                    reverseAnomalyLinks.computeIfAbsent(discoveredNodeId, k -> new HashSet<>()).add(sourceNodeId);
                }
            }
        }

        // Seed a queue for backward propagation. We start with all nodes that are currently known to be invalid
        // for ANY reason (disconnected or having a fatal leak).
        Queue<Integer> toBackPropagate = new LinkedList<>();
        
        // Add all nodes that were already marked as invalid from the "disconnected" pass above.
        toBackPropagate.addAll(invalidNodeIds);
        
        // Also add any nodes with a fatal leak, ensuring we don't add duplicates to the queue.
        for (BlobNode node : graph.values()) {
            if (node.hasFatalLeak) {
                // The .add() method of a Set returns true if the element was not already present.
                // This is a concise way to add to both the set and the queue simultaneously.
                if (invalidNodeIds.add(node.id)) {
                    toBackPropagate.add(node.id);
                }
            }
        }

        // Propagate the invalid status backwards through the anomaly links.
        while (!toBackPropagate.isEmpty()) {
            int invalidNodeId = toBackPropagate.poll();
            
            // Find which node(s) discovered this now-invalid node.
            Set<Integer> sources = reverseAnomalyLinks.get(invalidNodeId);
            if (sources == null) continue; // This node was not discovered via an anomaly.

            for (int sourceId : sources) {
                // If the discovering node is not already invalid, mark it as such and add it to the queue
                // to continue the backward propagation chain.
                if (invalidNodeIds.add(sourceId)) {
                    toBackPropagate.add(sourceId);
                }
            }
        }




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
                    BlockPos seed = data.position().above(d-1);//d-1 as we need a block below the hab block
                    initialRootSeeds.add(seed);
                    WorkItem workItem = new WorkItem(seed); 
                    workList.add(workItem);
                    break;
                }
            }
        }
    }

    private void scanAndProcessSeed(BlockPos seed, Level level) {
        if (isHab(level.getBlockState(seed))) {
            return;
        }

        BlobScanResult scanResult = discoverBlob(seed, level);
        
        //Create and process the node
        BlobNode node = new BlobNode();
        node.id = getNextNodeId();
        node.volume = scanResult.volume();
        node.hasFatalLeak = scanResult.hasLeak;
        graph.put(node.id, node);

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
                        if (!rleVolume.isInsideRleVolume(probePos)) {
                            node.hasFatalLeak = true;
                            break;
                        }
                    }

                    if (distanceToHab == -1) {
                        node.hasFatalLeak = true;
                    } else {
                        BlockPos anomalySeed = anomalyStartPos.above(distanceToHab - 1);
                        WorkItem workItem = new WorkItem(anomalySeed);
                        workList.add(workItem);

                        anomalySources.computeIfAbsent(node.id, k -> new HashSet<>()).add(anomalySeed);
                    }

                    // If an anomaly probe found a leak, we can stop checking for other anomalies.
                    if (node.hasFatalLeak) {
                        break; 
                    }
                }
            }
        }

        if (!node.hasFatalLeak) {
            for (BlockPos pos : node.volume) {
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
            // The node is leaky. Its only purpose is to occupy space to prevent re-scans.
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

                if (!rleVolume.isInsideRleVolume(neighborPos)) {
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

    public void checkAndPruneOrphanedBalloons(Level level) {
        if (finalizedBalloons.isEmpty() || graph.isEmpty()) {
            return; // Nothing to prune.
        }

        // --- Part A & B: Find all root nodes based on the CURRENT list of HAIs ---
        Set<BlockPos> currentSeeds = new HashSet<>();
        for (HaiData data : this.hais) {
            for (int d = 0; d < VERTICAL_PROBE_DISTANCE; d++) {
                BlockState nextBlockState = level.getBlockState(data.position().above(d));
                if (isHab(nextBlockState)) {
                    currentSeeds.add(data.position().above(d - 1));
                    break;
                }
            }
        }

        Set<Integer> rootNodeIds = new HashSet<>();
        for (BlockPos seed : currentSeeds) {
            if (blockToNodeId.containsKey(seed)) {
                rootNodeIds.add(blockToNodeId.get(seed));
            }
        }

        // --- Part C: Traverse the graph to find all currently reachable nodes ---
        Set<Integer> reachableNodeIds = new HashSet<>();
        Queue<Integer> toVisit = new LinkedList<>(rootNodeIds);
        reachableNodeIds.addAll(rootNodeIds);

        while (!toVisit.isEmpty()) {
            BlobNode currentNode = graph.get(toVisit.poll());
            if (currentNode == null) continue;

            for (int parentId : currentNode.parentIds) {
                if (reachableNodeIds.add(parentId)) toVisit.add(parentId);
            }
            for (int childId : currentNode.childrenIds) {
                if (reachableNodeIds.add(childId)) toVisit.add(childId);
            }
        }

        // --- Part D & E: Validate each balloon and prune orphans ---
        List<Balloon> validBalloons = new ArrayList<>();
        for (Balloon balloon : this.finalizedBalloons) {
            boolean isBalloonValid = true;
            for (BlockPos pos : balloon.interiorAir) {
                Integer nodeId = blockToNodeId.get(pos);
                if (nodeId == null || !reachableNodeIds.contains(nodeId)) {
                    isBalloonValid = false;
                    break;
                }
            }

            if (isBalloonValid) {
                validBalloons.add(balloon);
            }
        }

        // If the list of valid balloons is smaller, update the main list.
        if (validBalloons.size() < this.finalizedBalloons.size()) {
            this.finalizedBalloons.clear();
            this.finalizedBalloons.addAll(validBalloons);
        }
    }
}
