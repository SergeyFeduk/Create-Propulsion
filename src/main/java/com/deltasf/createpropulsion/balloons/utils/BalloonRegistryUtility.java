package com.deltasf.createpropulsion.balloons.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        BlockPos haiPos = data.position();

        for (HaiGroup group : haiGroups) {
            if (group.groupAABB != null && group.groupAABB.intersects(data.aabb())) {
                intersectingGroups.add(group);
                continue;
            }

            if (group.hais.isEmpty()) {
                synchronized(group.balloons) {
                    boolean connects = false;
                    for (Balloon b : group.balloons) {
                        // Check if this HAI is physically supporting this balloon
                        for (int d = 1; d <= HaiGroup.HAI_TO_BALLOON_DIST; d++) {
                            if (b.contains(haiPos.above(d))) {
                                connects = true;
                                break;
                            }
                        }
                        if (connects) break;
                    }
                    if (connects) {
                        intersectingGroups.add(group);
                    }
                }
            }
        }

        //Merge / Creation
        HaiGroup targetGroup;

        if (intersectingGroups.isEmpty()) {
            //Case 1: New Group
            targetGroup = new HaiGroup();
            targetGroup.hais.add(data);
            haiGroups.add(targetGroup);
        } else {
            //Case 2 & 3: Merge into primary
            targetGroup = intersectingGroups.get(0);
            targetGroup.hais.add(data);

            for(int i = 1; i < intersectingGroups.size(); i++) {
                HaiGroup groupToMerge = intersectingGroups.get(i);
                
                //Merge HAIs
                targetGroup.hais.addAll(groupToMerge.hais);
                for(HaiData hai : groupToMerge.hais) {
                    haiGroupMap.put(hai.id(), targetGroup);
                }
                
                for (Balloon balloonToMigrate : groupToMerge.balloons) {
                    targetGroup.adoptOrphanBalloon(balloonToMigrate);
                }
                
                haiGroups.remove(groupToMerge);
            }
        }

        haiGroupMap.put(data.id(), targetGroup);
        targetGroup.regenerateRLEVolume(level);
        
        synchronized(targetGroup.balloons) {
            for (Balloon b : targetGroup.balloons) {
                for (int d = 1; d <= HaiGroup.HAI_TO_BALLOON_DIST; d++) {
                    if (b.contains(haiPos.above(d))) {
                        b.supportHais.add(data.id());
                        break;
                    }
                }
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

        //Rule 1: balloon is fully contained within its group if group present (not zombie)
        if (group.groupAABB != null) {
            if (!isInside(group.groupAABB, balloon.getAABB())) return false;
        }

        //Rule 2 (partial): there must be at least one hai block below balloons bottom
        Set<UUID> currentGroupHais = group.hais.stream().map(HaiData::id).collect(Collectors.toSet()); //TODO: just store a set in haiGroup to avoid THIS
        //if (Collections.disjoint(balloon.supportHais, currentGroupHais)) return false;
        if (!balloon.supportHais.isEmpty() && Collections.disjoint(balloon.supportHais, currentGroupHais)) {
            return false;
        }

        return true;
    }

    public static boolean isInside(AABB outerBox, AABB innerBox) {
        if (outerBox == null || innerBox == null) return false;

        return outerBox.minX <= innerBox.minX &&
               outerBox.minY <= innerBox.minY &&
               outerBox.minZ <= innerBox.minZ &&
               outerBox.maxX >= innerBox.maxX &&
               outerBox.maxY >= innerBox.maxY &&
               outerBox.maxZ >= innerBox.maxZ;
    }

}
