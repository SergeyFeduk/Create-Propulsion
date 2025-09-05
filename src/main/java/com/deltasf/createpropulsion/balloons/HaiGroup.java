package com.deltasf.createpropulsion.balloons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.deltasf.createpropulsion.balloons.hot_air.HotAirSolver;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.balloons.utils.BalloonDebug;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner;
import com.deltasf.createpropulsion.balloons.utils.RLEVolume;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner.DiscoveredVolume;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HaiGroup {
    public static final int HAI_TO_BALLOON_DIST = 5;

    public final List<HaiData> hais = new ArrayList<>();
    public final List<Balloon> balloons = Collections.synchronizedList(new ArrayList<>());

    public RLEVolume rleVolume = new RLEVolume();
    public AABB groupAABB;

    public void scan(Level level) {
        //We shouldn't really recalculate the RLE volume here as it should be constantly valid, but this fails at some place in hai management logic
        //Needs to be fixed anyway
        regenerateRLEVolume();
        //Probe to get seeds
        //TODO: Do not seed hais that are associated with valid balloons. But think about that first
        List<BlockPos> seeds = new ArrayList<>();
        for(HaiData data : hais) {
            BlockPos seed = getSeedFromHai(data, level);
            if (seed != null) {
                seeds.add(seed);
            }
        }

        List<DiscoveredVolume> discoveredVolumes = BalloonScanner.scan(level, seeds, this, new ArrayList<>());
        generateBalloons(discoveredVolumes);
    }

    public void regenerateRLEVolume() {
        groupAABB = BalloonRegistryUtility.calculateGroupAABB(hais);
        rleVolume.regenerate(hais, groupAABB);
    }

    public void tickBalloons() {
        for(Balloon balloon : balloons) {
            HotAirSolver.tickBalloon(balloon);
        }
    }

    public static boolean isHab(BlockPos pos, Level level) {
        return level.getBlockState(pos).is(PropulsionBlocks.HAB_BLOCK.get());
    }

    public static BlockPos getSeedFromHai(HaiData data, Level level) {
        for(int d = 0; d < BalloonScanner.VERTICAL_ANOMALY_SCAN_DISTANCE; d++) {
            if (isHab(data.position().above(d), level)) {
                BlockPos seed = data.position().above(d-1);
                return seed;
            }
        }

        return null;
    }

    private void generateBalloons(List<DiscoveredVolume> discoveredVolumes) {
        this.balloons.clear();

        for(DiscoveredVolume discoveredVolume : discoveredVolumes) {
            if (discoveredVolume.isLeaky() || discoveredVolume.volume().isEmpty()) continue; //Leaky volume cannot become a balloon
            //Find support hais and obtain aabb
            Set<UUID> supportHais = findSupportHaisForVolume(discoveredVolume.volume());
            if (supportHais.isEmpty()) {
                continue;
            }
            //Create balloon
            Balloon balloon = new Balloon(discoveredVolume.volume(), null, supportHais);
            this.balloons.add(balloon);
        }
    }

    private Set<UUID> findSupportHaisForVolume(Set<BlockPos> volume) {
        Set<UUID> supporters = new HashSet<>();
        for (HaiData hai : this.hais) {
            for (int d = 1; d <= HAI_TO_BALLOON_DIST; d++) {
                BlockPos probePos = hai.position().above(d);
                if (volume.contains(probePos)) {
                    supporters.add(hai.id());
                    break;
                }
            }
        }
        return supporters;
    }

    public boolean isInsideRleVolume(BlockPos pos) {
        if (groupAABB == null) return false;
        int y = pos.getY() - (int) groupAABB.minY;
        int x = pos.getX() - (int) groupAABB.minX;

        if (y < 0 || y >= rleVolume.get().length || x < 0 || x >= rleVolume.get()[y].length) {
            return false;
        }

        List<Pair<Integer, Integer>> zIntervals = rleVolume.get()[y][x];
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
}
