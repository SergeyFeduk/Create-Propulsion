package com.deltasf.createpropulsion.balloons.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;

import net.minecraft.core.BlockPos;

public class HaiGroupingUtils {
    public static void addHaiAndRegroup(HaiData newData, List<HaiGroup> haiGroups, Map<UUID, HaiGroup> haiGroupMap) {
        List<HaiGroup> intersectingGroups = new ArrayList<>();
        for (HaiGroup group : haiGroups) {
            if (group.getRleVolume().getGroupAABB().intersects(newData.maxAABB())) {
                intersectingGroups.add(group);
            }
        }

        if (intersectingGroups.isEmpty()) {
            // Case 1: New HAI
            HaiGroup newGroup = new HaiGroup();
            newGroup.addHai(newData);
            newGroup.generateRLEVolume();
            haiGroups.add(newGroup);
            haiGroupMap.put(newData.id(), newGroup);

        } else if (intersectingGroups.size() == 1) {
            // Case 2: Add HAI to group
            HaiGroup targetGroup = intersectingGroups.get(0);
            targetGroup.addHai(newData);
            targetGroup.generateRLEVolume(); // Regenerate volume for the updated group
            haiGroupMap.put(newData.id(), targetGroup);

        } else {
            // Case 3: Add and merge intersecting groups
            HaiGroup primaryGroup = intersectingGroups.get(0);
            primaryGroup.addHai(newData);
            haiGroupMap.put(newData.id(), primaryGroup);

            // Merge other intersecting groups into the primary
            for (int i = 1; i < intersectingGroups.size(); i++) {
                HaiGroup groupToMerge = intersectingGroups.get(i);
                primaryGroup.addAllHais(groupToMerge.getHais());

                // Update the map for all moved HAIs to point to the new primary group
                for (HaiData haiInMergedGroup : groupToMerge.getHais()) {
                    haiGroupMap.put(haiInMergedGroup.id(), primaryGroup);
                }

                // Remove the merged group
                haiGroups.remove(groupToMerge);
            }
            
            primaryGroup.generateRLEVolume();
        }
    }

    public static boolean doesGroupSplit(List<HaiData> remainingHais) {
        if (remainingHais.size() <= 1) {
            return false; // A single HAI cannot be split
        }

        Set<HaiData> visited = new HashSet<>();
        Queue<HaiData> toCheck = new LinkedList<>();

        // Start a traversal from the first HAI
        toCheck.add(remainingHais.get(0));
        visited.add(remainingHais.get(0));

        while (!toCheck.isEmpty()) {
            HaiData current = toCheck.poll();
            for (HaiData potentialNeighbor : remainingHais) {
                if (!visited.contains(potentialNeighbor) && current.maxAABB().intersects(potentialNeighbor.maxAABB())) {
                    visited.add(potentialNeighbor);
                    toCheck.add(potentialNeighbor);
                }
            }
        }

        // If the number of visited HAIs is less than the total - we did not reach some of them
        return visited.size() < remainingHais.size();
    }

    public static void splitAndRecreateGroups(List<HaiData> haisToRegroup, List<HaiGroup> haiGroups, Map<UUID, HaiGroup> haiGroupMap) {
        List<HaiData> remainingHais = new ArrayList<>(haisToRegroup);

        while (!remainingHais.isEmpty()) {
            HaiGroup newSubGroup = new HaiGroup();
            Queue<HaiData> toCheck = new LinkedList<>();

            HaiData firstHai = remainingHais.remove(0);
            toCheck.add(firstHai);
            newSubGroup.addHai(firstHai);

            while (!toCheck.isEmpty()) {
                HaiData currentHai = toCheck.poll();
                List<HaiData> neighbors = new ArrayList<>();
                for (HaiData potentialNeighbor : remainingHais) {
                    if (potentialNeighbor.maxAABB().intersects(currentHai.maxAABB())) {
                        neighbors.add(potentialNeighbor);
                    }
                }
                remainingHais.removeAll(neighbors);
                for (HaiData neighbor : neighbors) {
                    newSubGroup.addHai(neighbor);
                    toCheck.add(neighbor);
                }
            }

            newSubGroup.generateRLEVolume();
            haiGroups.add(newSubGroup);
            for (HaiData haiData : newSubGroup.getHais()) {
                haiGroupMap.put(haiData.id(), newSubGroup);
            }
        }
    }

    public static boolean areBalloonsContainedInCurrentVolume(HaiGroup group) {

        if (group.getFinalizedBalloons().isEmpty()) {
            return true; // No balloons to invalidate
        }

        for (Balloon balloon : group.getFinalizedBalloons()) {
            for (BlockPos airBlock : balloon.interiorAir) {
                if (!group.getRleVolume().isInsideRleVolume(airBlock)) {
                    // Found a single air block that is now outside the boundary
                    return false;
                }
            }
        }

        // All balloon blocks are still inside the new volume
        return true;
    }
}
