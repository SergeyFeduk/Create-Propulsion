package com.deltasf.createpropulsion.balloons.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner.DiscoveredVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

//I'll touch your balloons and you can do nothing about it
public class BalloonStitcher {
    public static void createHole(Balloon balloon, BlockPos holePos) {
        balloon.holes.add(holePos);
    }

    public static void removeHole(Balloon balloon, BlockPos holePos) {
        balloon.holes.remove(holePos);
    }

    public static void extend(Balloon target, DiscoveredVolume extension) {
        target.addAll(extension.volume());
        target.resolveHolesAfterMerge();
    }

    public static void mergeInto(Balloon target, Balloon source, HaiGroup owner) {
        target.mergeFrom(source);
        owner.killBalloon(source);
        target.resolveHolesAfterMerge();
    }

    public static void handleSplit(Balloon originalBalloon, BlockPos splitPos, HaiGroup owner) {
        //Capture the density of hot air in original volume
        final double originalHotAir = originalBalloon.hotAir;
        final double originalVolumeSize = originalBalloon.getVolumeSize();
        final double hotAirDensity = (originalVolumeSize > 0) ? originalHotAir / originalVolumeSize : 0;

        // Step 1: Initial State Preparation
        originalBalloon.remove(splitPos);

        List<BlockPos> neighborSeeds = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = splitPos.relative(dir);
            if (originalBalloon.contains(neighbor)) {
                neighborSeeds.add(neighbor);
            }
        }

        // Early Exit 1: No split is possible if the placed block wasn't separating anything.
        if (neighborSeeds.size() <= 1) {
            validateHoles(originalBalloon);
            return;
        }

        // Step 2: DSU Construction
        DisjointSetUnion dsu = new DisjointSetUnion();
        Map<BlockPos, Integer> posToId = new HashMap<>();
        List<BlockPos> idToPos = originalBalloon.toList();
        for (int i = 0; i < idToPos.size(); i++) {
            posToId.put(idToPos.get(i), i);
            dsu.makeSet(i);
        }

        for (BlockPos pos : originalBalloon) {
            int posId = posToId.get(pos);
            // Check 3 neighbors to avoid redundant checks (East, Up, South)
            for (Direction dir : new Direction[]{Direction.EAST, Direction.UP, Direction.SOUTH}) {
                BlockPos neighbor = pos.relative(dir);
                if (originalBalloon.contains(neighbor)) {
                    dsu.union(posId, posToId.get(neighbor));
                }
            }
        }

        // Step 3: Component Identification
        Map<Integer, List<BlockPos>> rootToSeeds = new HashMap<>();
        for (BlockPos seed : neighborSeeds) {
            int rootId = dsu.find(posToId.get(seed));
            rootToSeeds.computeIfAbsent(rootId, k -> new ArrayList<>()).add(seed);
        }

        // Early Exit 2: All neighbors are still connected, so no split occurred.
        if (rootToSeeds.size() <= 1) {
            validateHoles(originalBalloon);
            return;
        }

        // --- A SPLIT HAS DEFINITELY OCCURRED ---

        // Step 4: Rebuild Volumes and Create New Balloons
        Map<Integer, Set<BlockPos>> rootToVolume = new HashMap<>();
        for (int i = 0; i < idToPos.size(); i++) {
            int rootId = dsu.find(i);
            rootToVolume.computeIfAbsent(rootId, k -> new HashSet<>()).add(idToPos.get(i));
        }

        owner.killBalloon(originalBalloon);
        List<Balloon> newBalloons = new ArrayList<>();

        for (Set<BlockPos> newVolume : rootToVolume.values()) {
            Set<UUID> newSupportHais = findSupportHaisForVolume(newVolume, owner.hais);
            Balloon newBalloon = owner.createBalloon(newVolume, newSupportHais);
            newBalloon.holes = partitionHoles(newVolume, originalBalloon.holes);
            validateHoles(newBalloon);
            newBalloon.hotAir = newBalloon.getVolumeSize() * hotAirDensity;
            newBalloons.add(newBalloon);
        }

        // Step 5: Finalization

        for (Balloon newBalloon : newBalloons) {
            newBalloon.isInvalid = !BalloonRegistryUtility.isBalloonValid(newBalloon, owner);
        }
    }

    // helpers

    public static Set<Balloon> findOverlappingBalloons(DiscoveredVolume dv, HaiGroup owner, Set<Balloon> excludedBalloons) {
        Set<Balloon> overlapping = new HashSet<>();
        if (dv.volume().isEmpty()) return overlapping;

        AABB dvBounds = calculateBoundsForVolume(dv.volume());

        for (Balloon candidate : owner.balloons) {
            if (excludedBalloons.contains(candidate) || !candidate.getAABB().intersects(dvBounds)) {
                continue;
            }

            for (BlockPos posInDv : dv.volume()) {
                if (candidate.contains(posInDv)) {
                    overlapping.add(candidate);
                    break; 
                }
            }
        }
        return overlapping;
    }

    private static Set<UUID> findSupportHaisForVolume(Set<BlockPos> volume, List<HaiData> hais) {
        Set<UUID> supporters = new HashSet<>();
        for (HaiData hai : hais) {
            for (int d = 1; d <= HaiGroup.HAI_TO_BALLOON_DIST; d++) {
                if (volume.contains(hai.position().above(d))) {
                    supporters.add(hai.id());
                    break;
                }
            }
        }
        return supporters;
    }

    //Iterates through all holes to check if they are no longer adjacent to balloon's volume. If so - removes them
    public static void validateHoles(Balloon balloon) {
        if (balloon.holes.isEmpty()) {
            return;
        }

        balloon.holes.removeIf(holePos -> {
            for (Direction dir : Direction.values()) {
                if (balloon.contains(holePos.relative(dir))) {
                    return false; //Holes is still adjacent to volume
                }
            }
            return true;
        });
    }

    private static Set<BlockPos> partitionHoles(Set<BlockPos> newVolume, Set<BlockPos> originalHoles) {
        Set<BlockPos> newHoles = new HashSet<>();
        for (BlockPos hole : originalHoles) {
            for (Direction dir : Direction.values()) {
                if (newVolume.contains(hole.relative(dir))) {
                    newHoles.add(hole);
                    break;
                }
            }
        }
        return newHoles;
    }

    private static AABB calculateBoundsForVolume(Set<BlockPos> volume) {
        if (volume.isEmpty()) return new AABB(0,0,0,0,0,0);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : volume) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
