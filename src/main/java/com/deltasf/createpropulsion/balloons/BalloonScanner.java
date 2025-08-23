package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BalloonScanner {
    public static final int VERTICAL_ANOMALY_SCAN_DISTANCE = 32;

    //External data
    private final List<HaiData> hais;
    private final RLEVolume rleVolume;
    private final Level level;

    public BalloonScanner(List<HaiData> hais, RLEVolume rleVolume, Level level) {
        this.hais = hais;
        this.rleVolume = rleVolume;
        this.level = level;
    }

    //Internal data
    private PriorityQueue<WorkItem> workList = new PriorityQueue<>((WorkItem a, WorkItem b) -> Integer.compare(b.y, a.y));
    private Map<Integer, BlobNode> graph = new HashMap<>();
    private Map<BlockPos, Integer> blockToNodeId = new HashMap<>();
    private Set<BlockPos> initialRootSeeds = new HashSet<>();
    private Map<Integer, Set<BlockPos>> anomalySources = new HashMap<>();
    private int nextNodeId = 0;
    private int getNextNodeId() { return nextNodeId++; }

    public FullScanResult scan() {
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
        return finalizeGraph();
    }
    
    private FullScanResult finalizeGraph() {
        // --- Phase 1: Invalidate nodes that are disconnected from any HAI seed ---
        Set<Integer> invalidNodeIds = new HashSet<>();
        Set<Integer> rootNodeIds = new HashSet<>();
        for (BlockPos seed : initialRootSeeds) {
            if (blockToNodeId.containsKey(seed)) {
                rootNodeIds.add(blockToNodeId.get(seed));
            }
        }

        Set<Integer> connectedToRoot = new HashSet<>();
        Queue<Integer> toVisitFromRoot = new LinkedList<>(rootNodeIds);
        connectedToRoot.addAll(rootNodeIds);

        while (!toVisitFromRoot.isEmpty()) {
            BlobNode currentNode = graph.get(toVisitFromRoot.poll());
            if (currentNode == null) continue;
            
            // Traverse all physical parent/child connections
            for (int parentId : currentNode.parentIds) {
                if (connectedToRoot.add(parentId)) toVisitFromRoot.add(parentId);
            }
            for (int childId : currentNode.childrenIds) {
                if (connectedToRoot.add(childId)) toVisitFromRoot.add(childId);
            }
        }

        for (Integer nodeId : graph.keySet()) {
            if (!connectedToRoot.contains(nodeId)) {
                invalidNodeIds.add(nodeId); // Mark disconnected nodes as invalid
            }
        }

        // --- Phase 2: Invalidate nodes connected to leaks or anomalies ---
        Map<Integer, Set<Integer>> reverseAnomalyLinks = new HashMap<>();
        for (Map.Entry<Integer, Set<BlockPos>> entry : anomalySources.entrySet()) {
            int sourceNodeId = entry.getKey();
            for (BlockPos discoveredSeed : entry.getValue()) {
                Integer discoveredNodeId = blockToNodeId.get(discoveredSeed);
                if (discoveredNodeId != null) {
                    reverseAnomalyLinks.computeIfAbsent(discoveredNodeId, k -> new HashSet<>()).add(sourceNodeId);
                }
            }
        }

        Queue<Integer> toBackPropagate = new LinkedList<>();
        toBackPropagate.addAll(invalidNodeIds); // Seed with already disconnected nodes
        
        // Also seed with any nodes that have a fatal leak
        for (BlobNode node : graph.values()) {
            if (node.hasFatalLeak) {
                if (invalidNodeIds.add(node.id)) {
                    toBackPropagate.add(node.id);
                }
            }
        }

        // Propagate invalidation backwards through the anomaly links
        while (!toBackPropagate.isEmpty()) {
            int invalidNodeId = toBackPropagate.poll();
            Set<Integer> sources = reverseAnomalyLinks.get(invalidNodeId);
            if (sources == null) continue;

            for (int sourceId : sources) {
                if (invalidNodeIds.add(sourceId)) {
                    toBackPropagate.add(sourceId);
                }
            }
        }

        // --- Phase 3: Propagate invalidation downwards from leaky nodes ---
        Queue<Integer> toPruneForward = new LinkedList<>();
        for (BlobNode node : graph.values()) {
            if (node.hasFatalLeak) { 
                // We already added these to invalidNodeIds, but we need to seed the forward propagation
                toPruneForward.add(node.id);
            }
        }

        while (!toPruneForward.isEmpty()) {
            int invalidId = toPruneForward.poll();
            BlobNode invalidNode = graph.get(invalidId);
            if (invalidNode == null) continue;
            for (Integer childrenId : invalidNode.childrenIds) {
                if (invalidNodeIds.add(childrenId)) {
                    toPruneForward.add(childrenId);
                }
            }
        }

        // --- Phase 4: Group all remaining valid nodes into balloons ---
        Set<Integer> visitedValidNodes = new HashSet<>();
        List<Set<BlockPos>> allBalloonVolumes = new ArrayList<>();


        for (BlobNode node : graph.values()) {
            if (invalidNodeIds.contains(node.id) || visitedValidNodes.contains(node.id)) {
                continue; // Skip invalid or already-grouped nodes
            }

            // Start of a new, valid balloon
            Set<BlockPos> currentBalloonVolume = new HashSet<>();
            Queue<Integer> toGroup = new LinkedList<>();
            
            toGroup.add(node.id);
            visitedValidNodes.add(node.id);

            // Traverse all connected valid nodes to gather the full volume of this balloon
            while (!toGroup.isEmpty()) {
                int currentId = toGroup.poll();
                BlobNode currentNode = graph.get(currentId);
                
                currentBalloonVolume.addAll(currentNode.volume);

                for (Integer parentId : currentNode.parentIds) {
                    if (!invalidNodeIds.contains(parentId) && visitedValidNodes.add(parentId)) {
                        toGroup.add(parentId);
                    }
                }
                for (Integer childrenId : currentNode.childrenIds) {
                    if (!invalidNodeIds.contains(childrenId) && visitedValidNodes.add(childrenId)) {
                        toGroup.add(childrenId);
                    }
                }
            }

            // Create a result for this complete balloon
            allBalloonVolumes.add(currentBalloonVolume);
        }

        return new FullScanResult(allBalloonVolumes, this.graph, this.blockToNodeId);
    }

    private void seedWorklistFromHais(Level level) {
        for(int i = 0; i < hais.size(); i++) {
            HaiData data = hais.get(i);
            
            for(int d = 0; d < VERTICAL_ANOMALY_SCAN_DISTANCE; d++) {
                BlockState nextBlockState = level.getBlockState(data.position().above(d));
                if (HaiGroup.isHab(nextBlockState)) {
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
        if (HaiGroup.isHab(level.getBlockState(seed))) {
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
                if (!isAboveDiscovered && !HaiGroup.isHab(level.getBlockState(above))) {
                    BlockPos anomalyStartPos = above;
                    int distanceToHab = -1;
                    for (int d = 0; d < VERTICAL_ANOMALY_SCAN_DISTANCE; d++) {
                        BlockPos probePos = anomalyStartPos.above(d);
                        BlockState nextBlockState = level.getBlockState(probePos);
                        if (HaiGroup.isHab(nextBlockState)) {
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
                if (HaiGroup.isHab(neighbourState)) {
                    continue; //Found a hab
                }

                volume.add(neighborPos);
                queue.add(neighborPos);
            }
        }

        return new BlobScanResult(volume, hasLeak);
    }

    //Classes
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

    public record FullScanResult(List<Set<BlockPos>> balloonVolumes, Map<Integer, BlobNode> graph,Map<BlockPos, Integer> blockToNodeId) {}
}
