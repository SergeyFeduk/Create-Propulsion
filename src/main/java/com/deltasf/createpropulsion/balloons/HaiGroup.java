package com.deltasf.createpropulsion.balloons;

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
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.envelopes.AbstractEnvelopeBlock;
import com.deltasf.createpropulsion.balloons.hot_air.HotAirSolver;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner;
import com.deltasf.createpropulsion.balloons.utils.ManagedHaiSet;
import com.deltasf.createpropulsion.balloons.utils.RLEVolume;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner.DiscoveredVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
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
            if (HotAirSolver.tickBalloon(level, balloon, this, registry, ship)) {
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
        //Yes I optimized out half of the safety checks
        LevelChunk chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        if (section.hasOnlyAir()) return false;
        BlockState state = section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        return state.getBlock() instanceof AbstractEnvelopeBlock;
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

    public void adoptOrphanBalloon(Balloon orphan) {
        Set<UUID> currentSupporterIds = new HashSet<>(orphan.supportHais);
        synchronized (balloons) {
            this.balloons.add(orphan);
        }
        Set<UUID> managedSet = new ManagedHaiSet(orphan, this.haiToBalloonMap, currentSupporterIds);
        orphan.supportHais = managedSet;
    }

    private void generateBalloons(List<DiscoveredVolume> discoveredVolumes) {
        for(DiscoveredVolume discoveredVolume : discoveredVolumes) {
            if (discoveredVolume.isLeaky() || discoveredVolume.volume().isEmpty()) continue; //Leaky volume cannot become a balloon
            //Find support hais and obtain aabb
            Set<UUID> supportHais = findSupportHaisForVolume(discoveredVolume.volume());
            if (supportHais.isEmpty()) { continue; }

            //Find all balloons that this volume connects to
            Set<Balloon> connectedBalloons = new HashSet<>();
            for (UUID haiId : supportHais) {
                Balloon existingBalloon = haiToBalloonMap.get(haiId);
                if (existingBalloon != null) {
                    connectedBalloons.add(existingBalloon);
                }
            }

            //Handle all cases
            if (connectedBalloons.isEmpty()) {
                //New balloon
                createBalloon(discoveredVolume.volume(), supportHais);
            } else if (connectedBalloons.size() == 1) {
                //Extend a balloon
                Balloon targetBalloon = connectedBalloons.iterator().next();

                targetBalloon.addAll(discoveredVolume.volume());
                targetBalloon.supportHais.addAll(supportHais);
                targetBalloon.resolveHolesAfterMerge();
            } else {
                //Merge multiple balloons
                List<Balloon> balloonsToMerge = new ArrayList<>(connectedBalloons);
                Balloon targetBalloon = balloonsToMerge.get(0);

                targetBalloon.addAll(discoveredVolume.volume());
                targetBalloon.supportHais.addAll(supportHais);

                for (int i = 1; i < balloonsToMerge.size(); i++) {
                    Balloon sourceBalloon = balloonsToMerge.get(i);
                    targetBalloon.mergeFrom(sourceBalloon);
                    killBalloon(sourceBalloon);
                }
                targetBalloon.resolveHolesAfterMerge();
            }
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

    public ServerShip getShip() {
        return ship;
    }
}
