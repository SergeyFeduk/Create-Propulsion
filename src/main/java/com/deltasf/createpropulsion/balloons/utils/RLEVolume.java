package com.deltasf.createpropulsion.balloons.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class RLEVolume {
    //Crazy idea: instead of storing this entire horrible structure - store just flattened out hai regions and bottom Y coordinates. 
    //As top Y is always equal to ship's top Y - no need to store this as "volume", but just as area bound from bottom

    //So we can store the Long2IntOpenHashMap where key is xz position packed into long and value is the minY coordinate at this position. 
    //However we probably do not need that much sparsity, so we can use flat array that stores the minY.
    //What I think is the best way is to use flat array [sizeX * sizeZ * sizeof(short)]. This will require 32MB for covering the 256x256 chunk ship, which is the worst case.

    private List<Pair<Integer, Integer>>[][] rleVolume;
    public List<Pair<Integer, Integer>>[][] get() {
        return rleVolume;
    }

    @SuppressWarnings("unchecked")
    public void regenerate(List<HaiData> hais, AABB groupAABB) {
        if (hais.isEmpty()) return;

        int sizeX = (int) (groupAABB.maxX - groupAABB.minX);
        int sizeY = (int) (groupAABB.maxY - groupAABB.minY);
        rleVolume = new List[sizeY][sizeX];

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                int worldX = (int) groupAABB.minX + x;
                int worldY = (int) groupAABB.minY + y;
                mutablePos.set(worldX, worldY, 0);

                List<Pair<Integer, Integer>> zIntervals = new ArrayList<>();

                //Collect z intervals
                for (HaiData hai : hais) {
                    if (hai.aabb().contains(mutablePos.getX(), mutablePos.getY(), hai.aabb().minZ)) {
                        zIntervals.add(Pair.of((int) hai.aabb().minZ, (int) hai.aabb().maxZ));
                    }
                }

                //Sort, merge and store intervals
                if (!zIntervals.isEmpty()) {
                    rleVolume[y][x] = mergeIntervals(zIntervals);
                }
            }
        }
    }

    private static List<Pair<Integer, Integer>> mergeIntervals(List<Pair<Integer, Integer>> intervals) {
        if (intervals.size() <= 1) {
            return intervals;
        }

        // Sort intervals by their starting point.
        intervals.sort(Comparator.comparing(Pair::getFirst));

        LinkedList<Pair<Integer, Integer>> merged = new LinkedList<>();
        for (Pair<Integer, Integer> interval : intervals) {
            // If merged list is empty or the current interval does not overlap with the previous, simply add it.
            if (merged.isEmpty() || merged.getLast().getSecond() < interval.getFirst() - 1) {
                merged.add(interval);
            }
            // Otherwise, there is an overlap, so we merge the current and previous intervals.
            else {
                Pair<Integer, Integer> last = merged.getLast();
                merged.set(merged.size() - 1, Pair.of(last.getFirst(), Math.max(last.getSecond(), interval.getSecond())));
            }
        }

        return merged;
    }
}
