package com.deltasf.createpropulsion.balloons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.hot_air.HotAirSolver;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.balloons.utils.BalloonDebug;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner;
import com.deltasf.createpropulsion.balloons.utils.ManagedHaiSet;
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
    private final Map<UUID, Balloon> haiToBalloonMap = new HashMap<>();


    public RLEVolume rleVolume = new RLEVolume();
    public AABB groupAABB;
    private ServerShip ship;

    public void scan(Level level) {
        //We shouldn't really recalculate the RLE volume here as it should be constantly valid, but this fails at some place in hai management logic
        //Needs to be fixed anyway
        regenerateRLEVolume(level);
        //Probe to get seeds
        //TODO: Do not seed hais that are associated with valid balloons. But think about that first
        List<BlockPos> seeds = new ArrayList<>();
        for(HaiData data : hais) {
            BlockPos seed = getSeedFromHai(data, level);
            if (seed != null) {
                //Do not seed from hai that already supports a balloon
                if (getBalloonFor(data) == null) {
                    seeds.add(seed);
                }
            }
        }

        List<DiscoveredVolume> discoveredVolumes = BalloonScanner.scan(level, seeds, this, new ArrayList<>());
        generateBalloons(discoveredVolumes);
    }

    public void regenerateRLEVolume(Level level) {
        groupAABB = BalloonRegistryUtility.calculateGroupAABB(hais);
        if (ship == null && hais.size() > 0) {
            ship = (ServerShip)VSGameUtilsKt.getShipManagingPos(level, hais.get(0).position());
        }
        rleVolume.regenerate(hais, groupAABB);
    }

    public void tickBalloons(Level level, BalloonRegistry registry) {
        final List<Balloon> balloonsToKill = new ArrayList<>();
        for(Balloon balloon : balloons) {
            if (HotAirSolver.tickBalloon(level, balloon, this, registry)) {
                balloonsToKill.add(balloon);
            }
        }

        synchronized(balloons) {
            for(Balloon balloon : balloonsToKill) {
                killBalloon(balloon);
            }
        }
    }

    public Balloon getBalloonFor(HaiData hai) {
        return haiToBalloonMap.get(hai.id());
    }

    public static boolean isHab(BlockPos pos, Level level) {
        return level.getBlockState(pos).is(PropulsionBlocks.ENVELOPE_BLOCK.get());
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

    public void killBalloon(Balloon balloon) {
        synchronized (balloons)  {
            if (balloons.remove(balloon)) {
                balloon.supportHais.clear();
            }
        }
    }

    public Balloon createBalloon(Set<BlockPos> volume, Set<UUID> supportHais) {
        Balloon balloon = new Balloon(volume, null);
        Set<UUID> managedSet = new ManagedHaiSet(balloon, this.haiToBalloonMap, new HashSet<>(supportHais));
        balloon.supportHais = managedSet;

        synchronized(balloons) {
            this.balloons.add(balloon);
        }

        return balloon;
    }

    public Balloon createManagedBalloonFromSave(double hotAir, Set<BlockPos> holes, long[] unpackedVolume, List<BlockPos> supportHaiPositions, Level level, BalloonRegistry registry) {
        //Pos's -> UUID's
        Set<UUID> supportHaiIds = new HashSet<>();
        for (BlockPos pos : supportHaiPositions) {
            BalloonRegistry.HaiData haiData = registry.getHaiAt(level, pos);
            if (haiData != null) {
                if (this == registry.getGroupOf(haiData.id())) {
                    supportHaiIds.add(haiData.id());
                }
            }
        }
        
        if (supportHaiIds.isEmpty()) {
            return null;
        }

        //Create balloon
        Balloon balloon = new Balloon(hotAir, holes, unpackedVolume);
        
        // Create and inject the ManagedHaiSet
        synchronized(balloons) {
            Set<UUID> managedSet = new ManagedHaiSet(balloon, this.haiToBalloonMap, supportHaiIds);
            balloon.supportHais = managedSet;
            this.balloons.add(balloon);
        }

        return balloon;
    }


    private void generateBalloons(List<DiscoveredVolume> discoveredVolumes) {
        //TODO: Do not clear balloon list, we only need to create new balloons here (and link hais under balloons to correct balloons)
        List<Balloon> balloonsToKill = new ArrayList<>(this.balloons);
        for (Balloon balloon : balloonsToKill) {
            killBalloon(balloon);
        }

        for(DiscoveredVolume discoveredVolume : discoveredVolumes) {
            if (discoveredVolume.isLeaky() || discoveredVolume.volume().isEmpty()) continue; //Leaky volume cannot become a balloon
            //Find support hais and obtain aabb
            Set<UUID> supportHais = findSupportHaisForVolume(discoveredVolume.volume());
            if (supportHais.isEmpty()) {
                continue;
            }
            //TODO: Check if one of those hais is related to some balloon. If it is - add this hai to that balloon
            //TODO: Otherwise - Create balloon

            createBalloon(discoveredVolume.volume(), supportHais);
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
        AABBic shipAABB = ship.getShipAABB();
        return rleVolume.isInside(pos.getX(), pos.getY(), pos.getZ(), groupAABB, shipAABB);
    }
}
