package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.blocks.HaiBlockEntity;
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
    private final List<HaiGroup> haiGroups = new ArrayList<>();

    public void registerHai(UUID haiId, HaiBlockEntity hai) {
        probeAndRegroup(haiId, hai.getLevel(), hai.getBlockPos());
    }

    public List<HaiGroup> getHaiGroups() {
        return haiGroups;
    }


    public void unregisterHai(UUID haiId) {
        if (haiDataMap.remove(haiId) != null) {
            // If a HAI was actually removed, we need to recompute the groups
            recomputeGroups();
        }
    }

    private void recomputeGroups() {
        haiGroups.clear();
        List<HaiData> remainingHais = new ArrayList<>(haiDataMap.values());

        while (!remainingHais.isEmpty()) {
            HaiGroup newGroup = new HaiGroup();
            Queue<HaiData> toCheck = new LinkedList<>();

            HaiData firstHai = remainingHais.remove(0);
            newGroup.addHai(firstHai);
            toCheck.add(firstHai);
            while (!toCheck.isEmpty()) {
                HaiData currentHai = toCheck.poll();

                // Find all neighbors of the current HAI from the remaining list.
                List<HaiData> neighbors = new ArrayList<>();
                for (HaiData potentialNeighbor : remainingHais) {
                    if (potentialNeighbor.maxAABB().intersects(currentHai.maxAABB())) {
                        neighbors.add(potentialNeighbor);
                    }
                }

                // Move the found neighbors from the remaining list to our new group and the queue.
                remainingHais.removeAll(neighbors);
                for (HaiData neighbor : neighbors) {
                    newGroup.addHai(neighbor);
                    toCheck.add(neighbor);
                }
            }
            newGroup.generateRleVolume();
            haiGroups.add(newGroup);
        }
    }

    public void startScanFor(UUID haiId, Level level, BlockPos position) {
        //Redo vertical probe and grouping
        probeAndRegroup(haiId, level, position);
        //Actually find a relevant haiGroup. I should really unretard this
        for (HaiGroup haiGroup : haiGroups) {
            // A more efficient way to check if the group contains the HAI
            if (haiGroup.getHais().stream().anyMatch(data -> data.id().equals(haiId))) {
                haiGroup.scan(level);
                return;
            }
        }
    }

    private void probeAndRegroup(UUID haiId, Level level, BlockPos blockPos) {
        int probeResult = initialVerticalProbe(level, blockPos);
        if (probeResult == -1 || probeResult == 0) {
            // This HAI is invalid, do not register it for scanning.
            // You might want to log this or have a state on the BE.
            return;
        }
        AABB maxAabb = getMaxAABB(probeResult, blockPos);
        HaiData data = new HaiData(haiId, blockPos, maxAabb);
        haiDataMap.put(haiId, data);

        recomputeGroups();
    }


    //Static helper methods for vertical probing

    //Returns distance to the LAST met HAB block above the hai. If no block found - returns -1
    //This is the best compromise I found
    private static int initialVerticalProbe(Level level, BlockPos origin) {
        //Obtain a ship
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, origin);
        if (ship == null) return -1;
        //Obtain the topmost hab block
        int maxY = ship.getShipAABB().maxY();
        int highestHabY = -1;
        for(int y = origin.getY(); y < maxY; y++) {
            BlockState currenBlockState = level.getBlockState(new BlockPos(origin.getX(), y, origin.getZ()));
            if (HaiGroup.isHab(currenBlockState)) {
                highestHabY = y;
            }
        }
        if (highestHabY == -1) return -1; //Did not find a block, vertical probe failed
        return highestHabY - origin.getY();
    }

    public static AABB getMaxAABB(int probeResult, BlockPos origin) {
        int halfExtents = BalloonShipRegistry.MAX_HORIZONTAL_SCAN / 2;
        int halfExtentsMod = BalloonShipRegistry.MAX_HORIZONTAL_SCAN % 2;
        BlockPos posStart = new BlockPos(origin.getX() - halfExtents, origin.getY() + 1, origin.getZ() - halfExtents);
        BlockPos posEnd = new BlockPos(origin.getX() + halfExtents + halfExtentsMod, origin.getY() + 1 + probeResult, origin.getZ() + halfExtents + halfExtentsMod);

        return new AABB(posStart, posEnd);
    }
}
