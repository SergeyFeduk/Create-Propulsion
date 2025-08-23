package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.deltasf.createpropulsion.balloons.blocks.HaiBlockEntity;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanUtils;
import com.deltasf.createpropulsion.balloons.utils.HaiGroupingUtils;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BalloonRegistry {
    public BalloonRegistry() {}
    public record HaiData(UUID id, BlockPos position, AABB maxAABB) {}
    private final Map<UUID, HaiData> haiDataMap = new ConcurrentHashMap<>();
    private final Map<UUID, HaiGroup> haiGroupMap = new ConcurrentHashMap<>();
    private final List<HaiGroup> haiGroups = new ArrayList<>();

    public List<HaiGroup> getHaiGroups() {
        return haiGroups;
    }

    public void registerHai(UUID haiId, HaiBlockEntity hai) {
        Level level = hai.getLevel();
        BlockPos blockPos = hai.getBlockPos();

        int probeResult = BalloonScanUtils.initialVerticalProbe(level, blockPos);
        if (probeResult <= 0) { // This HAI is invalid or found no blocks
            // If it was previously registered, its removal might cause a split.
            unregisterHai(haiId, level);
            return;
        }

        AABB maxAabb = BalloonScanUtils.getMaxAABB(probeResult, blockPos);
        HaiData newData = new HaiData(haiId, blockPos, maxAabb);

        // If the HAI already exists, unregister it first to handle position changes correctly.
        if (haiDataMap.containsKey(haiId)) {
            unregisterHai(haiId, level);
        }
        
        haiDataMap.put(haiId, newData);
        HaiGroupingUtils.addHaiAndRegroup(newData, haiGroups, haiGroupMap);
    }

    public void unregisterHai(UUID haiId, Level level) {
        HaiData removedHai = haiDataMap.remove(haiId);
        if (removedHai == null) {
            return; // Not registered.
        }

        // Find the group this HAI belonged to using its ID.
        HaiGroup affectedGroup = haiGroupMap.get(haiId);
        
        // Now we can remove the HAI's specific mapping from the map.
        haiGroupMap.remove(haiId);

        if (affectedGroup == null) {
            return; // The HAI existed but wasn't part of a group.
        }

        // Remove the HAI from its group's internal list.
        affectedGroup.getHais().remove(removedHai);

        // If the group is now empty, it's simple: just remove it.
        if (affectedGroup.getHais().isEmpty()) {
            haiGroups.remove(affectedGroup);
            return;
        }

        // --- Safety Checks (Corrected Order) ---

        // Check 1: Did removing the HAI split the group into multiple pieces? This is the most severe outcome.
        if (HaiGroupingUtils.doesGroupSplit(affectedGroup.getHais())) {
            // Unsafe: The group is no longer contiguous.
            // We must destroy the old group and create new ones for each split piece.
            haiGroups.remove(affectedGroup);
            HaiGroupingUtils.splitAndRecreateGroups(affectedGroup.getHais(), haiGroups, haiGroupMap);

        } else {
            // The group is still connected.
            // Check 2 (NEW ORDER): First, perform the intelligent logical check to prune any orphaned balloons.
            affectedGroup.checkAndPruneOrphanedBalloons(level);

            // Check 3 (NEW ORDER): Now, perform the geometric check on the REMAINING balloons.
            // This ensures that the survivors are still within the new, smaller boundaries.
            if (!HaiGroupingUtils.doBalloonsFitInNewVolume(affectedGroup)) {
                // Unsafe: A surviving balloon was breached by the shrinking volume.
                // This is an edge case, but we must invalidate the state if it happens.
                affectedGroup.invalidateBalloonState();
            }
            // If both checks pass, the group's state is now correctly and minimally updated.
        }
    }

    public void startScanFor(UUID haiId, Level level, BlockPos position) {
        // First, ensure the HAI is correctly registered and its group is up-to-date.
        // We get the BlockEntity from the world to pass to registerHai.
        if (level.getBlockEntity(position) instanceof HaiBlockEntity hai) {
            // This call performs the probe and non-destructively updates the groups.
            this.registerHai(haiId, hai);
        } else {
            // The block entity doesn't exist or is the wrong type. We can't scan.
            // It might be a good idea to ensure it's unregistered.
            unregisterHai(haiId, level);
            return;
        }

        // The rest of the logic remains the same.
        HaiGroup haiGroup = haiGroupMap.get(haiId);
        if (haiGroup != null) {
            haiGroup.scan(level);
        }
    }
}
