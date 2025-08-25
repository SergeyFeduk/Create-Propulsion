package com.deltasf.createpropulsion.balloons.registries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.utils.DisjointSetUnion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class BalloonProcessor {
    public static void processBlockPlacement(Level level, BlockPos pos) {

        final Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship == null) {
            return; // Not on a ship, no balloons to split.
        }

        // Get the specific registry for this ship.
        final BalloonRegistry registry = BalloonShipRegistry.forShip(ship.getId());

        findAffectedBalloon(registry, pos).ifPresent(context -> {
            Balloon affectedBalloon = context.balloon();

            //Handle volume removal
            Balloon.BalloonSegment affectedSegment = affectedBalloon.blockToSegmentMap.get(pos);
            if (affectedSegment == null) return;
            affectedBalloon.interiorAir.remove(pos);
            affectedBalloon.blockToSegmentMap.remove(pos);
            affectedSegment.volume.remove(pos);

            Collection<Set<BlockPos>> splitVolumes = detectSplit(context.balloon(), pos);

            // Check if a split occurred.
            if (splitVolumes.size() > 1) {
                System.out.println("SPLIT DETECTED! Balloon split into " + splitVolumes.size() + " parts.");
                // TODO: Here you will resolve the split. You have everything you need:
                // context.group() -> The HaiGroup to modify.
                // context.balloon() -> The original balloon to be removed.
                // splitVolumes -> A collection of BlockPos sets, one for each new balloon.
            }
        });
    }

    private static Optional<AffectedBalloonContext> findAffectedBalloon(BalloonRegistry registry, BlockPos pos) {
        for (HaiGroup group : registry.getHaiGroups()) {
            for (Balloon balloon : group.getFinalizedBalloons()) {
                if (balloon.interiorAir.contains(pos)) {
                    return Optional.of(new AffectedBalloonContext(group, balloon));
                }
            }
        }
        return Optional.empty();
    }

    private static Collection<Set<BlockPos>> detectSplit(Balloon targetBalloon, BlockPos splitPoint) {
        DisjointSetUnion dsu = new DisjointSetUnion();
        Map<BlockPos, Integer> posToId = new HashMap<>();
        int idCounter = 0;

        // A. Initialize DSU for each segment, skipping the split point.
        // This part is the same and is correct.
        for (BlockPos pos : targetBalloon.interiorAir) {
            if (pos.equals(splitPoint)) {
                continue; // This is the "cut".
            }
            posToId.put(pos, idCounter);
            dsu.makeSet(idCounter);
            idCounter++;
        }

        // B. Form Unions based on 3D ADJACENCY, not the gameplay DAG.
        // This is the corrected logic.
        for (BlockPos currentPos : posToId.keySet()) {
            int currentId = posToId.get(currentPos);
            
            // Check all 6 neighbors (up, down, north, east, south, west).
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(dir);

                // If the neighbor is also a valid part of the balloon (and not the split point)...
                if (posToId.containsKey(neighborPos)) {
                    // ...connect them in the disjoint set.
                    dsu.union(currentId, posToId.get(neighborPos));
                }
            }
        }

        // C. Group the segments by their final root ID.
        // This part is the same and is correct.
        Map<Integer, Set<BlockPos>> resultingVolumes = new HashMap<>();
        for (Map.Entry<BlockPos, Integer> entry : posToId.entrySet()) {
            int root = dsu.find(entry.getValue());
            resultingVolumes.computeIfAbsent(root, k -> new HashSet<>()).add(entry.getKey());
        }

        return resultingVolumes.values();
    }
    private record AffectedBalloonContext(HaiGroup group, Balloon balloon) {}
}
