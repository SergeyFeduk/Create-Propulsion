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

import com.deltasf.createpropulsion.balloons.BalloonScanner.Balloon;
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
        BalloonScanner.ScanResult result = scanner.scan();

        finalizedBalloons = result.balloons();
        graph = result.graph();
        blockToNodeId = result.blockToNodeId();
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
