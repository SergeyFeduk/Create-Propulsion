package com.deltasf.createpropulsion.balloons.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public  class BalloonScanner {
    public record DiscoveredVolume(Set<BlockPos> volume, boolean isLeaky) {};
    public static final int VERTICAL_ANOMALY_SCAN_DISTANCE = 32;

    public static List<DiscoveredVolume> scan(Level level, List<BlockPos> seeds, HaiGroup group, Collection<Balloon> excludedBalloons) {
        ScanState state = new ScanState();
        state.group = group;
        state.originalSeeds = seeds;
        state.excludedBalloons = excludedBalloons;

        //Fill worklist from given seeds
        for(BlockPos seed : seeds) {
            state.workList.add(new WorkItem(seed));
        }

        //Phase 1: Scan worklist and construct DAG
        while (!state.workList.isEmpty()) {
            WorkItem currentWork = state.workList.poll();
            if (isExcluded(currentWork.seed, state) || state.blockToNodeId.containsKey(currentWork.seed)) {
                continue;
            }

            scanAndProcessWorkItem(currentWork.seed, level, state);
        }

        //Phase 2: finalize the graph
        return finalizeGraph(state);
    }

    private static void scanAndProcessWorkItem(BlockPos origin, Level level, ScanState state) {
        if (HaiGroup.isHab(origin, level)) return;

        BlobScanResult scanResult = discoverBlob(origin, level, state);

        BlobNode node = new BlobNode();
        node.id = state.getNextNodeId();
        node.volume = scanResult.volume();
        node.hasFatalLeak = scanResult.hasLeak;
        state.graph.put(node.id, node);

        if (!node.hasFatalLeak) {
            //Discover anomalies
            for(BlockPos pos : node.volume) {
                BlockPos above = pos.above();
                boolean isAboveDiscovered = state.blockToNodeId.containsKey(above);

                //Discover the anomaly
                if (!isAboveDiscovered && !HaiGroup.isHab(above, level)) {
                    int distanceTohab = -1;
                    for(int d = 0; d < VERTICAL_ANOMALY_SCAN_DISTANCE; d++) {
                        BlockPos probePos = above.above(d);
                        if (HaiGroup.isHab(probePos, level)) {
                            distanceTohab = d;
                            break;
                        }
                        if (!state.group.isInsideRleVolume(probePos)) {
                            node.hasFatalLeak = true;
                            break;
                        }
                    }

                    if (distanceTohab == -1) {
                        node.hasFatalLeak = true;
                    } else {
                        //Add the anomaly into worklist
                        BlockPos anomalyPos = above.above(distanceTohab - 1);
                        WorkItem anomalyItem = new WorkItem(anomalyPos);
                        state.workList.add(anomalyItem);

                        state.anomalySources.computeIfAbsent(node.id, k -> new HashSet<>()).add(anomalyPos);
                    }

                    // If an anomaly probe found a leak, we can stop checking for other anomalies
                    if (node.hasFatalLeak) {
                        break;
                    }
                }
            }
        }

        if (!node.hasFatalLeak) {
            for(BlockPos pos : node.volume) {
                state.blockToNodeId.put(pos, node.id);
                //Find parents
                BlockPos above = pos.above();
                if (state.blockToNodeId.containsKey(above)) {
                    int parentId = state.blockToNodeId.get(above);
                    node.parentIds.add(parentId);
                    BlobNode parent = state.graph.get(parentId);
                    if (parent != null) parent.childrenIds.add(node.id);
                }
                
                //Find children
                BlockPos below = pos.below();
                if (state.blockToNodeId.containsKey(below)) {
                    int childId = state.blockToNodeId.get(below);
                    node.childrenIds.add(childId);
                    BlobNode child = state.graph.get(childId);
                    if (child != null) child.parentIds.add(node.id);
                }

                //Add blocks below to worklist
                WorkItem workItem = new WorkItem(below);
                state.workList.add(workItem);
            } 
        } else {
            //Node is leaky. It will only occupy space
            for(BlockPos pos : node.volume) {
                state.blockToNodeId.put(pos, node.id);
            }
        }
    }

    private static BlobScanResult discoverBlob(BlockPos origin, Level level, ScanState state) {
        Set<BlockPos> volume = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        boolean hasLeak = false;

        if (HaiGroup.isHab(origin, level)) {
            return new BlobScanResult(volume, false);
        }
        if (!state.group.isInsideRleVolume(origin)) {
            return new BlobScanResult(volume, true);
        }

        queue.add(origin);
        volume.add(origin);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                mutablePos.set(currentPos).move(dir);
                BlockPos neighborPos = mutablePos;

                if (volume.contains(neighborPos) || state.blockToNodeId.containsKey(neighborPos)) {
                    continue;
                }

                if (!state.group.isInsideRleVolume(neighborPos)) {
                    hasLeak = true;
                    continue;
                }

                if (isExcluded(neighborPos, state)) {
                    continue;
                }

                if (HaiGroup.isHab(neighborPos, level)) {
                    continue;
                }

                BlockPos immutableCopy = neighborPos.immutable(); 
                volume.add(immutableCopy);
                queue.add(immutableCopy);
            }
        }

        return new BlobScanResult(volume, hasLeak);
    }

    private static List<DiscoveredVolume> finalizeGraph(ScanState state) {
        //Step 0: Initialize structures
        Set<Integer> leakyNodeIds = new HashSet<>();
        Set<Integer> seedNodeIds = new HashSet<>();

        for(BlockPos pos : state.originalSeeds) {
            if (state.blockToNodeId.containsKey(pos)) {
                seedNodeIds.add(state.blockToNodeId.get(pos));
            }
        }

        //Step 1: Invalidate orphan nodes (ones not reachable from any seeds)
        Set<Integer> connectedToSeed = new HashSet<>();
        Queue<Integer> toVisitFromSeeds = new ArrayDeque<>(seedNodeIds);
        connectedToSeed.addAll(seedNodeIds);

        while (!toVisitFromSeeds.isEmpty()) {
            BlobNode currentNode = state.graph.get(toVisitFromSeeds.poll());
            if (currentNode == null) continue;

            //Traverse all parent/children connections
            for(int parentId : currentNode.parentIds) {
                if (connectedToSeed.add(parentId)) toVisitFromSeeds.add(parentId);
            }

            for(int childrenId : currentNode.childrenIds) {
                if (connectedToSeed.add(childrenId)) toVisitFromSeeds.add(childrenId);
            }
        }

        //Mark them as leaky
        for(Integer nodeId : state.graph.keySet()) {
            if (!connectedToSeed.contains(nodeId)) {
                leakyNodeIds.add(nodeId);
            }
        }

        //Step 2: Resolve leaky anomaly connections
        Map<Integer, Set<Integer>> reverseAnomalyLinks = new HashMap<>();
        for(Map.Entry<Integer, Set<BlockPos>> entry : state.anomalySources.entrySet()) {
            int sourceNodeId = entry.getKey();
            for (BlockPos discoveredSeed : entry.getValue()) {
                Integer discoveredNodeId = state.blockToNodeId.get(discoveredSeed);
                if (discoveredNodeId != null) {
                    reverseAnomalyLinks.computeIfAbsent(discoveredNodeId, k -> new HashSet<>()).add(sourceNodeId);
                }
            }
        }

        //Start from already known leaky nodes
        Queue<Integer> toBackPropagate = new ArrayDeque<>();
        toBackPropagate.addAll(leakyNodeIds);
        //And add to this all nodes with discovered leaks
        for(BlobNode node : state.graph.values()) {
            if (node.hasFatalLeak) {
                if (leakyNodeIds.add(node.id)) {
                    toBackPropagate.add(node.id);
                }
            }
        }

        //Propagate invalidation backwards from anomaly links
        while (!toBackPropagate.isEmpty()) {
            int leakyNodeId = toBackPropagate.poll();
            Set<Integer> sources = reverseAnomalyLinks.get(leakyNodeId);
            if (sources == null) continue;

            for(int sourceId : sources) {
                if (leakyNodeIds.add(sourceId)) {
                    toBackPropagate.add(sourceId);
                }
            } 
        }

        //Step 3: Propagate leakiness downwards
        Queue<Integer> toPruneForward = new ArrayDeque<>();
        for(BlobNode node : state.graph.values()) {
            if (node.hasFatalLeak) {
                //This is already in leakyNodeIds, but we need to prune forwards (downwards)
                toPruneForward.add(node.id);
            }
        }

        while (!toPruneForward.isEmpty()) {
            int leakyid = toPruneForward.poll();
            BlobNode leakyNode = state.graph.get(leakyid);
            if (leakyNode == null) continue;
            for(Integer childrenId : leakyNode.childrenIds) {
                if (leakyNodeIds.add(childrenId)) {
                    toPruneForward.add(childrenId);
                }
            }
        }

        //Step 4: Segment volumes
        List<DiscoveredVolume> discoveredVolumes = new ArrayList<>();
        Set<Integer> visitedNodes = new HashSet<>();

        for(Integer startNodeId : state.graph.keySet()) {
            if (visitedNodes.contains(startNodeId)) continue;
            //Found a new unprocessed volume, start traversal
            Set<BlockPos> currentVolume = new HashSet<>();
            boolean isCurrentVolumeLeaky = leakyNodeIds.contains(startNodeId);
            Queue<Integer> toVisit = new ArrayDeque<>();
            toVisit.add(startNodeId);
            visitedNodes.add(startNodeId);
            
            while(!toVisit.isEmpty()) {
                int currentId = toVisit.poll();
                BlobNode currentNode = state.graph.get(currentId);
                if (currentNode == null) continue;

                currentVolume.addAll(currentNode.volume);

                //Check all neighbours
                Set<Integer> neighbours = new HashSet<>();
                neighbours.addAll(currentNode.parentIds);
                neighbours.addAll(currentNode.childrenIds);

                for(Integer neighborId : neighbours) {
                    if (visitedNodes.contains(neighborId)) continue;

                    boolean isNeighborLeaky = leakyNodeIds.contains(neighborId);
                    if (isNeighborLeaky == isCurrentVolumeLeaky) {
                        visitedNodes.add(neighborId);
                        toVisit.add(neighborId);
                    }
                }
            }
            //Add this volume to the list
            if (connectedToSeed.contains(startNodeId)) {
                discoveredVolumes.add(new DiscoveredVolume(currentVolume, isCurrentVolumeLeaky));
            }
        }

        return discoveredVolumes;
    }

    private static boolean isExcluded(BlockPos pos, ScanState state) {
        if (state.excludedBalloons == null) return false;
        for (Balloon balloon : state.excludedBalloons) {
            if (balloon.contains(pos)) {
                return true;
            }
        }
        return false;
    }


    public static class ScanState {
        
        public PriorityQueue<WorkItem> workList = new PriorityQueue<>((WorkItem a, WorkItem b) -> Integer.compare(b.y, a.y));
        public Map<Integer, BlobNode> graph = new HashMap<>();
        public Map<BlockPos, Integer> blockToNodeId = new HashMap<>();
        public Collection<Balloon> excludedBalloons;

        //Populated during discovery of the anomaly. Used to propagate leakiness through anomalies
        public Map<Integer, Set<BlockPos>> anomalySources = new HashMap<>(); 
        //Populated on start of the scan. Used to mark disconnected nodes
        public List<BlockPos> originalSeeds; 
        public HaiGroup group;
        int nextNodeId = 0;
        public int getNextNodeId() {return nextNodeId++;}
    }

    public static class WorkItem {
        public BlockPos seed;
        public int y;
        public WorkItem(BlockPos seed) {
            this.seed = seed;
            this.y = seed.getY();
        }
    }

    private static record BlobScanResult(Set<BlockPos> volume, boolean hasLeak) {}

    public static class BlobNode {
        public int id;
        public Set<BlockPos> volume = new HashSet<>();
        public Set<Integer> parentIds = new HashSet<>();
        public Set<Integer> childrenIds = new HashSet<>();
        boolean hasFatalLeak;
    }

    //On performance

    //Updating from PriorityQueue does not yield better performance, tested
    //Caching isHab makes it even worse :(
    //Replacing BlockPos decreases GC load and improves performance a little. I tried it, but it is questionable so I scraped this for now
    
    //I: I had early exit planned (if out of RLE volume - stop the scan immediately), but need to impl it still. I also need to be very fucking accurate cus
    // naive implementation won't work as we are seeding downwards and will still rediscover most of the volume if done naively
}
