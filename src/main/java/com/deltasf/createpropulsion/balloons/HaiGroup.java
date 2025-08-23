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
        Map<BlockPos, Balloon.BalloonSegment> segments = new HashMap<>();
        List<Balloon.BalloonSegment> bottomLayerSegments = new ArrayList<>();

        // First pass: Create a segment object for every block in this specific balloon's volume
        for (BlockPos pos : balloonVolume) {
            segments.put(pos, new Balloon.BalloonSegment(pos));
        }

        // Second pass: Link the segments together into a DAG using the graph data stored in this HaiGroup
        for (Balloon.BalloonSegment segment : segments.values()) {
            // Use the HaiGroup's 'blockToNodeId' map
            Integer nodeId = this.blockToNodeId.get(segment.pos);
            if (nodeId == null) continue;

            // Use the HaiGroup's 'graph'
            BalloonScanner.BlobNode node = this.graph.get(nodeId);
            if (node == null) continue;

            // Link to all parents (segments directly above)
            for (int parentId : node.parentIds) {
                BalloonScanner.BlobNode parentNode = this.graph.get(parentId);
                if (parentNode != null) {
                    BlockPos parentPos = segment.pos.above();
                    // Check if the parent block is part of THIS balloon's volume
                    if (segments.containsKey(parentPos)) {
                        segment.parents.add(segments.get(parentPos));
                    }
                }
            }

            // Link to all children (segments directly below)
            for (int childId : node.childrenIds) {
                BalloonScanner.BlobNode childNode = this.graph.get(childId);
                if (childNode != null) {
                    BlockPos childPos = segment.pos.below();
                    // Check if the child block is part of THIS balloon's volume
                    if (segments.containsKey(childPos)) {
                        segment.children.add(segments.get(childPos));
                    }
                }
            }
        }
        
        // Third pass: Identify the bottom-most segments for gameplay logic, now that the DAG is built
        for (Balloon.BalloonSegment segment : segments.values()) {
            // A segment is at the bottom of the physical structure if it has no children within this balloon
            if (segment.children.isEmpty()) {
                bottomLayerSegments.add(segment);
            }
        }

        return new Balloon(balloonVolume, segments, bottomLayerSegments);
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
