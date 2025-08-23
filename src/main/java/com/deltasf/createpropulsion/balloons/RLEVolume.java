package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.deltasf.createpropulsion.balloons.BalloonRegistry.HaiData;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class RLEVolume {
    private List<HaiData> hais;
    private List<Pair<Integer, Integer>>[][] rleVolume;
    private AABB groupAABB;

    public List<Pair<Integer, Integer>>[][] get() {
        return rleVolume;
    }

    public AABB getGroupAABB () {return groupAABB; }

    public void setHais(List<HaiData> newHais) {
        hais = newHais;
    }

    //TODO: Optimize so it DOES NOT use the entire bounding AABB. 
    @SuppressWarnings("unchecked") // For the generic array creation
    public void generateRleVolume() {
        if (hais.isEmpty()) {
            return;
        }

        // 1. Calculate the single bounding box for the whole group.
        this.groupAABB = hais.get(0).maxAABB();
        for (int i = 1; i < hais.size(); i++) {
            this.groupAABB = this.groupAABB.minmax(hais.get(i).maxAABB());
        }

        int sizeX = (int) (groupAABB.maxX - groupAABB.minX);
        int sizeY = (int) (groupAABB.maxY - groupAABB.minY);
        rleVolume = new List[sizeY][sizeX]; // Initialize the 2D array

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // 2. Loop through every Y/X column in the group's AABB.
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                int worldX = (int) groupAABB.minX + x;
                int worldY = (int) groupAABB.minY + y;
                mutablePos.set(worldX, worldY, 0);

                List<Pair<Integer, Integer>> zIntervals = new ArrayList<>();

                // 3. Collect all Z-intervals from HAIs that cover this column.
                for (BalloonRegistry.HaiData hai : hais) {
                    if (hai.maxAABB().contains(mutablePos.getX(), mutablePos.getY(), hai.maxAABB().minZ)) {
                        zIntervals.add(Pair.of((int) hai.maxAABB().minZ, (int) hai.maxAABB().maxZ));
                    }
                }

                // 4. Sort and merge the intervals, then store them.
                if (!zIntervals.isEmpty()) {
                    rleVolume[y][x] = mergeIntervals(zIntervals);
                }
            }
        }
    }

    public boolean isInsideRleVolume(BlockPos pos) {
        if (groupAABB == null) return false;
        int y = pos.getY() - (int) groupAABB.minY;
        int x = pos.getX() - (int) groupAABB.minX;

        if (y < 0 || y >= rleVolume.length || x < 0 || x >= rleVolume[y].length) {
            return false;
        }

        List<Pair<Integer, Integer>> zIntervals = rleVolume[y][x];
        if (zIntervals == null || zIntervals.isEmpty()) {
            return false;
        }

        int worldZ = pos.getZ();
        for (Pair<Integer, Integer> interval : zIntervals) {
            if (worldZ >= interval.getFirst() && worldZ <= interval.getSecond()) {
                return true;
            }
        }
        return false;
    }

    private static List<Pair<Integer, Integer>> mergeIntervals(List<Pair<Integer, Integer>> intervals) {
        if (intervals.size() <= 1) {
            return intervals;
        }

        // Sort intervals by their starting point.
        intervals.sort(Comparator.comparing(Pair::getFirst));

        LinkedList<Pair<Integer, Integer>> merged = new LinkedList<>();
        for (Pair<Integer, Integer> interval : intervals) {
            // If merged list is empty or the current interval does not overlap with the previous,
            // simply add it.
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
