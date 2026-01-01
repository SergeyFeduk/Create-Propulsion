package com.deltasf.createpropulsion.balloons.utils;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
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

    public static class SliceScannerContext {
        final Queue<BlockPos> queue = new ArrayDeque<>();
        final Set<BlockPos> visited = new HashSet<>();
        final Set<BlockPos> sliceVolume = new HashSet<>();
        final Set<BlockPos> sliceHoles = new HashSet<>();
        final Set<BlockPos> sliceShell = new HashSet<>();

        void clear() {
            queue.clear();
            visited.clear();
            sliceVolume.clear();
            sliceHoles.clear();
            sliceShell.clear();
        }
    }

    private static final SliceScanResult EMPTY_RESULT = new SliceScanResult(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false, -1);

    public static SliceScanResult scan(Level level, Balloon balloon, BlockPos seed, SliceScannerContext context) {
        if (!balloon.contains(seed)) {
            return EMPTY_RESULT;
        }
        context.clear();

        final int yLevel = seed.getY();
        context.queue.add(seed);
        context.visited.add(seed);

        while (!context.queue.isEmpty()) {
            BlockPos currentPos = context.queue.poll();
            if (balloon.contains(currentPos)) {
                BlockPos below = currentPos.below();
                if (balloon.contains(below)) {
                    context.sliceVolume.add(currentPos);
                    return new SliceScanResult(new HashSet<>(context.sliceVolume), new HashSet<>(context.sliceHoles), new HashSet<>(context.sliceShell), false, yLevel);
                }
                context.sliceVolume.add(currentPos);
            } else if (balloon.containsHoleAt(currentPos)) {
                context.sliceHoles.add(currentPos);
            }

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = currentPos.relative(dir);

                if (context.visited.contains(neighbor)) {
                    continue;
                }

                if (balloon.contains(neighbor) || balloon.containsHoleAt(neighbor)) {
                    context.visited.add(neighbor);
                    context.queue.add(neighbor);
                } else if (HaiGroup.isHab(neighbor, level)) {
                    context.visited.add(neighbor);
                    context.sliceShell.add(neighbor);
                }
            }
        }
        return new SliceScanResult(new HashSet<>(context.sliceVolume), new HashSet<>(context.sliceHoles), new HashSet<>(context.sliceShell), true, yLevel);
    }
}
