package com.deltasf.createpropulsion.balloons.registries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * A central utility class to process block changes related to balloons.
 * This is called directly from the HabBlock's onPlace and onRemove methods.
 */
public class BalloonProcessor {
    /*
    private static final int MAX_LEAK_SCAN_ITERATIONS = 64;

    public record InvalidationResult(boolean hasLeaked, @Nullable BlockPos leakPosition, int blocksScanned) {
        public static InvalidationResult leakedByIterationLimit(int count) {
            return new InvalidationResult(true, null, count);
        }
        public static InvalidationResult leakedAtPos(BlockPos pos, int count) {
            return new InvalidationResult(true, pos, count);
        }
        public static InvalidationResult noLeak(int count) {
            return new InvalidationResult(false, null, count);
        }
    }


    public static void processBlockChange(Level level, BlockPos pos, BlockState oldState, BlockState newState, boolean isMoving) {
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship == null) { return; }
        if (isMoving) { System.out.println("DEBUG: HAB block change at " + pos + " was caused by piston movement."); }
        boolean wasHab = HaiGroup.isHab(oldState);
        boolean isHab = HaiGroup.isHab(newState);
        if (wasHab == isHab) { return; }
        boolean isBreak = wasHab && !isHab;
        BalloonRegistry registry = BalloonShipRegistry.forShip(ship.getId());

        // --- START OF CORRECTED AABB-BASED GROUP DETECTION ---
        for (HaiGroup group : registry.getHaiGroups()) {
            AABB groupAABB = group.getGroupAABB();
            if (groupAABB == null) continue;

            // Step 1: Broad-phase check against the group's AABB, expanded 1 block upwards.
            // .expandTowards() is used to only inflate in the positive Y direction.
            AABB inflatedGroupAABB = groupAABB.expandTowards(0, 1, 0);

            // Check if the block position is within this expanded area of influence.
            if (inflatedGroupAABB.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {

                // Step 2: Narrow-phase check against the individual HAI AABBs, also expanded upwards.
                // This ensures the block is relevant to a specific part of the group, not just empty space in the group AABB.
                boolean foundMatchInGroup = false;
                for (BalloonRegistry.HaiData hai : group.getHais()) {
                    AABB inflatedHaiAABB = hai.maxAABB().expandTowards(0, 1, 0);
                    if (inflatedHaiAABB.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                        foundMatchInGroup = true;
                        break;
                    }
                }

                if (foundMatchInGroup) {
                    findAndProcessAffectedBalloons(group, pos, isBreak, level);
                    return; // Found the correct group and processed, our work is done.
                }
            }
        }
        // --- END OF CORRECTED LOGIC ---
    }


    /**
     * The final filtering stage. Checks if the block change actually affects a balloon's shell.
     
    private static void findAndProcessAffectedBalloons(HaiGroup group, BlockPos pos, boolean isBreak, Level level) {
        if (isBreak) {
            List<HaiGroup.Balloon> affectedBalloons = new ArrayList<>();
            for (HaiGroup.Balloon balloon : group.getFinalizedBalloons()) {
                if (balloon.shell.contains(pos)) {
                    affectedBalloons.add(balloon);
                }
            }
            if (!affectedBalloons.isEmpty()) {
                System.out.println("PREREQ: Block BREAK detected on the shell of " + affectedBalloons.size() + " balloon(s) at " + pos);
                for (HaiGroup.Balloon balloon : affectedBalloons) {
                    balloon.shell.remove(pos);
                }
                System.out.println("ACTION: Removing broken block from " + affectedBalloons.size() + " shell set(s).");
                if (isOnlyFloorBlock(pos, affectedBalloons)) {
                    System.out.println("DETECTION (PRE-CHECK): Broken block was a floor-only segment. No leak possible. Skipping scan.");
                    // TODO: Implement "On Block Break (Merging)" sub-algorithm here.
                    return;
                }

                Set<BlockPos> allBalloonsAirInGroup = group.getFinalizedBalloons().stream()
                        .flatMap(b -> b.interiorAir.stream())
                        .collect(Collectors.toSet());
                InvalidationResult result = checkForLeak(group, pos, level, allBalloonsAirInGroup);
                
                if (result.hasLeaked()) {
                    if (result.leakPosition() != null) {
                        System.out.println("DETECTION: Leak found! Scan escaped RLE volume at " + result.leakPosition() + " after scanning " + result.blocksScanned() + " blocks.");
                    } else {
                        System.out.println("DETECTION: Leak found! Scan reached iteration limit of " + MAX_LEAK_SCAN_ITERATIONS + " blocks.");
                    }
                    System.out.println("ACTION: Deleting " + affectedBalloons.size() + " affected balloon(s).");
                    group.getFinalizedBalloons().removeAll(affectedBalloons);
                } else {
                    System.out.println("DETECTION: No leak found. Scanned " + result.blocksScanned() + " blocks.");
                    // TODO: Implement "On Block Break (Merging)" sub-algorithm here if no leak was found.
                }
            }

        } else { // isPlace
            Set<HaiGroup.Balloon> adjacentBalloons = new HashSet<>();
            for (HaiGroup.Balloon balloon : group.getFinalizedBalloons()) {
                for (Direction dir : Direction.values()) {
                    if (balloon.interiorAir.contains(pos.relative(dir))) {
                        adjacentBalloons.add(balloon);
                        break;
                    }
                }
            }
            if (!adjacentBalloons.isEmpty()) {
                System.out.println("PREREQ: Block PLACE detected adjacent to " + adjacentBalloons.size() + " balloon(s) at " + pos);
                // TODO: Implement "On Block Place (Splitting & Merging)" sub-algorithm here.
            }
        }
    }


    private static boolean isOnlyFloorBlock(BlockPos pos, List<HaiGroup.Balloon> affectedBalloons) {
        boolean foundNeighborAbove = false;
        boolean foundNeighborNotAbove = false;

        for (HaiGroup.Balloon balloon : affectedBalloons) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (balloon.interiorAir.contains(neighbor)) {
                    if (neighbor.getY() > pos.getY()) {
                        foundNeighborAbove = true;
                    } else {
                        // Any neighbor at the same level or below trips this flag.
                        foundNeighborNotAbove = true;
                    }
                }
            }
        }
        // If we found a connection above AND we found no connections elsewhere, it's a floor block.
        return foundNeighborAbove && !foundNeighborNotAbove;
    }


    private static InvalidationResult checkForLeak(HaiGroup group, BlockPos startPos, Level level, Set<BlockPos> allBalloonsAir) {
        System.out.println("DEBUG: Starting Bounded-Leak Scan (no downward travel) at " + startPos);

        Queue<BlockPos> toVisit = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        toVisit.add(startPos);
        visited.add(startPos);

        while (!toVisit.isEmpty()) {
            if (visited.size() >= MAX_LEAK_SCAN_ITERATIONS) {
                return InvalidationResult.leakedByIterationLimit(visited.size());
            }
            BlockPos currentPos = toVisit.poll();

            for (Direction dir : Direction.values()) {
                // THE CRITICAL RULE CHANGE: Hot air doesn't leak down.
                if (dir == Direction.DOWN) {
                    continue;
                }

                BlockPos neighborPos = currentPos.relative(dir);
                if (visited.contains(neighborPos)) continue;
                if (allBalloonsAir.contains(neighborPos)) continue;
                if (!group.isInsideRleVolume(neighborPos)) {
                    return InvalidationResult.leakedAtPos(neighborPos, visited.size());
                }
                BlockState neighborState = level.getBlockState(neighborPos);
                if (!HaiGroup.isHab(neighborState)) {
                    visited.add(neighborPos);
                    toVisit.add(neighborPos);
                }
            }
        }
        return InvalidationResult.noLeak(visited.size());
    }

*/
}
