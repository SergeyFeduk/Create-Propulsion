package com.deltasf.createpropulsion.balloons.utils;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public final class SliceScanner {
    private SliceScanner() {}

    public record SliceScanResult(
        Set<BlockPos> sliceVolume,
        Set<BlockPos> sliceHoles,
        Set<BlockPos> sliceShell,
        boolean isBottomMostLayer,
        int yLevel
    ) {}

    private static final SliceScanResult EMPTY_RESULT = new SliceScanResult(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false, -1);

    public static SliceScanResult scan(Level level, Balloon balloon, BlockPos seed) {
        if (!balloon.contains(seed)) {
            return EMPTY_RESULT;
        }

        final int yLevel = seed.getY();
        final Queue<BlockPos> queue = new LinkedList<>();
        final Set<BlockPos> visited = new HashSet<>();
        
        final Set<BlockPos> sliceVolume = new HashSet<>();
        final Set<BlockPos> sliceHoles = new HashSet<>();
        final Set<BlockPos> sliceShell = new HashSet<>();

        queue.add(seed);
        visited.add(seed);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            if (balloon.contains(currentPos)) {
                sliceVolume.add(currentPos);
                BlockPos below = currentPos.below();
                //This is NOT a bottom most layer. Early exit
                if (balloon.contains(below)) {
                    return new SliceScanResult(sliceVolume, sliceHoles, sliceShell, false, yLevel);
                }
            } else if (balloon.holes.contains(currentPos)) {
                sliceHoles.add(currentPos);
            }

            //Expand slice
            for (Direction dir : Direction.Plane.HORIZONTAL)  {
                BlockPos neighbor = currentPos.relative(dir);

                if (visited.contains(neighbor)) {
                    continue;
                }

                //Category theory
                if (balloon.contains(neighbor) || balloon.holes.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                } else if (HaiGroup.isHab(neighbor, level)) {
                    visited.add(neighbor);
                    sliceShell.add(neighbor);
                }
            }
        }
        return new SliceScanResult(sliceVolume, sliceHoles, sliceShell, true, yLevel);
    }
}
