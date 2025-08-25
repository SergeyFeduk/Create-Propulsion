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

import com.deltasf.createpropulsion.balloons.BalloonScanner.BlobNode;
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
    private final RLEVolume rleVolume = new RLEVolume();

    private Map<Integer, BalloonScanner.BlobNode> graph = new HashMap<>();
    private Map<BlockPos, Integer> blockToNodeId = new HashMap<>();
    public List<Balloon> finalizedBalloons = new ArrayList<>();

    public void addHai(HaiData data) {
        hais.add(data);
    }

    public void addAllHais(List<HaiData> newHais) {
        hais.addAll(newHais);
    }

    public List<BalloonRegistry.HaiData> getHais() {
        return hais;
    }

    public void invalidateBalloonState() {
        finalizedBalloons.clear();
    }

    public List<Balloon> getFinalizedBalloons() {
        return finalizedBalloons;
    }

    public RLEVolume getRleVolume() {
        return rleVolume;
    }

    public void generateRLEVolume() {
        rleVolume.setHais(hais);
        rleVolume.generateRleVolume();
    }

    public static boolean isHab(BlockState state) {
        return state.is(PropulsionBlocks.HAB_BLOCK.get());
    }

    public void scan(Level level) {
        BalloonScanner scanner = new BalloonScanner(hais, rleVolume, level);
        BalloonScanner.FullScanResult rawResult = scanner.scan();

        finalizedBalloons.clear();
        graph = rawResult.graph();
        blockToNodeId = rawResult.blockToNodeId();

        for (Set<BlockPos> balloonVolume : rawResult.balloonVolumes()) {
            this.finalizedBalloons.add(translateScanResult(balloonVolume));
        }
    }

    private Balloon translateScanResult(Set<BlockPos> balloonVolume) {
        Map<Integer, Balloon.BalloonSegment> nodeIdToSegmentMap = new HashMap<>();
        
        // First pass: Create a BalloonSegment for each relevant BlobNode
        for (Map.Entry<Integer, BalloonScanner.BlobNode> entry : this.graph.entrySet()) {
            Integer nodeId = entry.getKey();
            BalloonScanner.BlobNode node = entry.getValue();
            
            // Ensure this node is part of the current balloon we are processing
            // We check if the first block of the node's volume is in our target set.
            if (!node.volume.isEmpty() && balloonVolume.contains(node.volume.iterator().next())) {
                nodeIdToSegmentMap.put(nodeId, new Balloon.BalloonSegment(node.volume));
            }
        }

        // Second pass: Link the newly created segments together into a DAG
        for (Map.Entry<Integer, Balloon.BalloonSegment> entry : nodeIdToSegmentMap.entrySet()) {
            Integer nodeId = entry.getKey();
            Balloon.BalloonSegment segment = entry.getValue();
            BalloonScanner.BlobNode node = this.graph.get(nodeId);

            // Link to parents
            for (int parentId : node.parentIds) {
                if (nodeIdToSegmentMap.containsKey(parentId)) {
                    segment.parents.add(nodeIdToSegmentMap.get(parentId));
                }
            }
            // Link to children
            for (int childId : node.childrenIds) {
                if (nodeIdToSegmentMap.containsKey(childId)) {
                    segment.children.add(nodeIdToSegmentMap.get(childId));
                }
            }
        }

        // Third pass: Build the final data structures for the Balloon object
        List<Balloon.BalloonSegment> allSegments = new ArrayList<>(nodeIdToSegmentMap.values());
        Map<BlockPos, Balloon.BalloonSegment> blockToSegmentMap = new HashMap<>();
        List<Balloon.BalloonSegment> bottomLayerSegments = new ArrayList<>();

        for (Balloon.BalloonSegment segment : allSegments) {
            // Populate the reverse-lookup map
            for (BlockPos pos : segment.volume) {
                blockToSegmentMap.put(pos, segment);
            }
            // Identify bottom layers for gameplay
            if (segment.children.isEmpty()) {
                bottomLayerSegments.add(segment);
            }
        }

        return new Balloon(balloonVolume, allSegments, blockToSegmentMap, bottomLayerSegments);
    }



    public void checkAndPruneOrphanedBalloons(Level level) {
        if (finalizedBalloons.isEmpty() || graph.isEmpty()) {
            return; // Nothing to prune.
        }

        // --- Part A & B: Find all root nodes based on the CURRENT list of HAIs ---
        Set<BlockPos> currentSeeds = new HashSet<>();
        for (HaiData data : hais) {
            for (int d = 0; d < BalloonScanner.VERTICAL_ANOMALY_SCAN_DISTANCE; d++) {
                BlockState nextBlockState = level.getBlockState(data.position().above(d));
                if (HaiGroup.isHab(nextBlockState)) {
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
        for (Balloon balloon : finalizedBalloons) {
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
        if (validBalloons.size() < finalizedBalloons.size()) {
            finalizedBalloons.clear();
            finalizedBalloons.addAll(validBalloons);
        }
    }
}
