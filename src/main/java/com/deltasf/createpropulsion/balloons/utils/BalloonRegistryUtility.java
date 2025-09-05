package com.deltasf.createpropulsion.balloons.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.valkyrienskies.core.impl.shadow.da;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class BalloonRegistryUtility {

    /**
     * Returns AABB for the given hai block. 
     * @param probeResult - vertical extent
     */
    public static AABB getHaiAABB(int probeResult, BlockPos origin) {
        int halfExtents = BalloonShipRegistry.MAX_HORIZONTAL_SCAN / 2;
        int halfExtentsMod = BalloonShipRegistry.MAX_HORIZONTAL_SCAN % 2;
        BlockPos posStart = new BlockPos(origin.getX() - halfExtents, origin.getY() + 1, origin.getZ() - halfExtents);
        BlockPos posEnd = new BlockPos(origin.getX() + halfExtents + halfExtentsMod, origin.getY() + 1 + Math.max(1, probeResult), origin.getZ() + halfExtents + halfExtentsMod);

        return new AABB(posStart, posEnd);
    }

    /**
     * Traverses the group to find if all elements are reachable. If not - it has split
     */
    public static boolean didGroupSplit(List<HaiData> group) {
        if (group.size() <= 1) {
            return false; // A single HAI cannot be split
        }

        Set<HaiData> visited = new HashSet<>();
        Queue<HaiData> toCheck = new LinkedList<>();

        // Start a traversal from the first HAI
        toCheck.add(group.get(0));
        visited.add(group.get(0));

        while (!toCheck.isEmpty()) {
            HaiData current = toCheck.poll();
            for (HaiData potentialNeighbor : group) {
                if (!visited.contains(potentialNeighbor) && current.aabb().intersects(potentialNeighbor.aabb())) {
                    visited.add(potentialNeighbor);
                    toCheck.add(potentialNeighbor);
                }
            }
        }

        // If the number of visited HAIs is less than the total - we did not reach some of them
        return visited.size() < group.size();
    }

    public static void addHaiAndRegroup(HaiData data, List<HaiGroup> haiGroups, Map<UUID, HaiGroup> haiGroupMap, Level level) {
        List<HaiGroup> intersectingGroups = new ArrayList<>();

        for (HaiGroup group : haiGroups) {
            if (group.groupAABB.intersects(data.aabb())) {
                intersectingGroups.add(group);
            }
        }

        if (intersectingGroups.isEmpty()) {
            //Case 1: New hai group
            HaiGroup group = new HaiGroup();
            group.hais.add(data);
            group.regenerateRLEVolume(level);
            haiGroups.add(group);
            haiGroupMap.put(data.id(), group);
        } else if (intersectingGroups.size() == 1) {
            //Case 2: Add hai to a single group
            HaiGroup group = intersectingGroups.get(0);
            group.hais.add(data);
            group.regenerateRLEVolume(level);
            haiGroupMap.put(data.id(), group);
        } else {
            //Case 3: Add hai and merge intersecting groups
            HaiGroup primaryGroup = intersectingGroups.get(0);
            primaryGroup.hais.add(data);
            haiGroupMap.put(data.id(), primaryGroup);

            //Merge other intersecting groups into the primary one
            for(int i = 1; i < intersectingGroups.size(); i++) {
                HaiGroup groupToMerge = intersectingGroups.get(i);
                primaryGroup.hais.addAll(groupToMerge.hais);

                //Update the map for all moved hais to point to the primary group
                for(HaiData hai : groupToMerge.hais) {
                    haiGroupMap.put(hai.id(), primaryGroup);
                }

                //Migrate all balloons too
                primaryGroup.balloons.addAll(groupToMerge.balloons);

                //Remove merged group
                haiGroups.remove(groupToMerge);
            }
        }
    }

    public static AABB calculateGroupAABB(List<HaiData> group) {
        if (group == null || group.isEmpty()) {
            return null; 
        }
        AABB combinedAABB = group.get(0).aabb();
        for (int i = 1; i < group.size(); i++) {
            HaiData currentHai = group.get(i);
            combinedAABB = combinedAABB.minmax(currentHai.aabb());
        }
        return combinedAABB;
    }

    public static List<HaiGroup> splitAndRecreateGroups(List<HaiData> group, List<HaiGroup> haiGroups, Map<UUID, HaiGroup> haiGroupMap, Level level) {
        List<HaiData> remainingHais = new ArrayList<>(group);
        List<HaiGroup> generatedSubGroups = new ArrayList<>();

        while (!remainingHais.isEmpty()) {
            HaiGroup newSubGroup = new HaiGroup();
            Queue<HaiData> toCheck = new LinkedList<>();

            HaiData firstHai = remainingHais.remove(0);
            toCheck.add(firstHai);
            newSubGroup.hais.add(firstHai);

            while (!toCheck.isEmpty()) {
                HaiData currentHai = toCheck.poll();
                List<HaiData> neighbours = new ArrayList<>();
                for (HaiData potentialNeighbour : remainingHais) {
                    if (potentialNeighbour.aabb().intersects(currentHai.aabb())) {
                        neighbours.add(potentialNeighbour);
                    }
                }

                remainingHais.removeAll(neighbours);
                for(HaiData neighbour : neighbours) {
                    newSubGroup.hais.add(neighbour);
                    toCheck.add(neighbour);
                }
            }
            //Finalize new sub group
            newSubGroup.regenerateRLEVolume(level);
            haiGroups.add(newSubGroup);
            for(HaiData hai : newSubGroup.hais) {
                haiGroupMap.put(hai.id(), newSubGroup);
            }
            generatedSubGroups.add(newSubGroup);
        }

        return generatedSubGroups;
    }

    public static boolean isBalloonValid(Balloon balloon, HaiGroup group) {
        //Rule 3: balloon is not empty
        if (balloon.isEmpty()) return false;

        //Rule 1: balloon is fully contained within its group
        if (!isInside(group.groupAABB, balloon.getAABB())) return false;

        //Rule 2 (partial): there must be at least one hai block below balloons bottom
        Set<UUID> currentGroupHais = group.hais.stream().map(HaiData::id).collect(Collectors.toSet()); //TODO: just store a set in haiGroup to avoid THIS
        if (Collections.disjoint(balloon.supportHais, currentGroupHais)) return false;

        return true;
    }

    public static boolean isInside(AABB outerBox, AABB innerBox) {
        return outerBox.minX <= innerBox.minX &&
               outerBox.minY <= innerBox.minY &&
               outerBox.minZ <= innerBox.minZ &&
               outerBox.maxX >= innerBox.maxX &&
               outerBox.maxY >= innerBox.maxY &&
               outerBox.maxZ >= innerBox.maxZ;
    }

}
