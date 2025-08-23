package com.deltasf.createpropulsion.balloons.registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import com.deltasf.createpropulsion.balloons.HaiGroup;
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

    private final Map<UUID, HaiData> haiDataMap = new HashMap<>();
    private final Map<UUID, HaiGroup> haiGroupMap = new HashMap<>();
    private final List<HaiGroup> haiGroups = new ArrayList<>();

    public List<HaiGroup> getHaiGroups() {
        return haiGroups;
    }

    public void registerHai(UUID haiId, HaiBlockEntity hai) {
        Level level = hai.getLevel();
        BlockPos blockPos = hai.getBlockPos();

        int probeResult = BalloonScanUtils.initialVerticalProbe(level, blockPos);
        if (probeResult <= 0) { // HAI is invalid or found no blocks, kill
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
        if (removedHai == null) { return; }

        //Kill
        HaiGroup affectedGroup = haiGroupMap.get(haiId);
        haiGroupMap.remove(haiId);

        if (affectedGroup == null) { return; } //This should not happen
        affectedGroup.getHais().remove(removedHai);

        // If the group is emptied - kill
        if (affectedGroup.getHais().isEmpty()) {
            haiGroups.remove(affectedGroup);
            return;
        }

        //Do to the group what it deserves

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

            affectedGroup.generateRLEVolume();
            // Check 3 (NEW ORDER): Now, perform the geometric check on the REMAINING balloons.
            // This ensures that the survivors are still within the new, smaller boundaries.
            if (!HaiGroupingUtils.areBalloonsContainedInCurrentVolume(affectedGroup)) {
                // Unsafe: A surviving balloon was breached by the shrinking volume.
                // This is an edge case, but we must invalidate the state if it happens.
                affectedGroup.invalidateBalloonState();
            }
        }
    }

    public void startScanFor(UUID haiId, Level level, BlockPos position) {
        if (level.getBlockEntity(position) instanceof HaiBlockEntity hai) {
            this.registerHai(haiId, hai);
        } else {
            unregisterHai(haiId, level);
            return;
        }

        HaiGroup haiGroup = haiGroupMap.get(haiId);
        if (haiGroup != null) {
            haiGroup.scan(level);
        }
    }
}
