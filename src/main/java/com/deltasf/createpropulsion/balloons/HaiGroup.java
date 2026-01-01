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

import com.deltasf.createpropulsion.balloons.envelopes.IEnvelope;
import com.deltasf.createpropulsion.balloons.hot_air.HotAirSolver;
import com.deltasf.createpropulsion.balloons.network.BalloonSyncManager;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner;
import com.deltasf.createpropulsion.balloons.utils.BalloonStitcher;
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

    public void scan(Level level, BalloonRegistry registry) {
        regenerateRLEVolume(level);
        List<BlockPos> seeds = new ArrayList<>();
        for(HaiData data : hais) {
            BlockPos seed = getSeedFromHai(data, level);
            if (seed != null) {
                seeds.add(seed);
            }
        }

        List<DiscoveredVolume> discoveredVolumes = BalloonScanner.scan(level, seeds, this, new ArrayList<>());
        generateBalloons(discoveredVolumes, registry);
    }

    public void regenerateRLEVolume(Level level) {
        groupAABB = BalloonRegistryUtility.calculateGroupAABB(hais);
        if (ship == null) {
            if (!hais.isEmpty()) {
                ship = (ServerShip)VSGameUtilsKt.getShipManagingPos(level, hais.get(0).position());
            } else if (!balloons.isEmpty()) {
                // Fallback for zombie groups
                Balloon b = balloons.get(0);
                if (!b.isEmpty()) {
                    ship = (ServerShip)VSGameUtilsKt.getShipManagingPos(level, b.iterator().next());
                }
            }
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
        LevelChunk chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        if (section.hasOnlyAir()) return false;
        BlockState state = section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        return ((IEnvelope)state.getBlock()).isEnvelope();
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

                if (this.ship != null) {
                    BalloonSyncManager.pushDestroy(this.ship.getId(), balloon.id);
                }
            }
        }
    }

    public Balloon createBalloon(Set<BlockPos> volume, Set<UUID> supportHais, BalloonRegistry registry) {
        int newId = registry.nextBalloonId();
        Balloon balloon = new Balloon(newId, volume, null);
        Set<UUID> managedSet = new ManagedHaiSet(balloon, this.haiToBalloonMap, new HashSet<>(supportHais));
        balloon.supportHais = managedSet;

        synchronized(balloons) {
            this.balloons.add(balloon);
        }

        return balloon;
    }

    public Balloon createManagedBalloonFromSave(double hotAir, Set<BlockPos> holes, long[] unpackedVolume, List<BlockPos> supportHaiPositions, Level level, BalloonRegistry registry) {
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

        int newId = registry.nextBalloonId();
        Balloon balloon = new Balloon(newId, hotAir, holes, unpackedVolume);
        
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

    private void generateBalloons(List<DiscoveredVolume> discoveredVolumes, BalloonRegistry registry) {
        for(DiscoveredVolume discoveredVolume : discoveredVolumes) {
            if (discoveredVolume.isLeaky() || discoveredVolume.volume().isEmpty()) continue;
            
            Set<UUID> supportHais = findSupportHaisForVolume(discoveredVolume.volume());
            if (supportHais.isEmpty()) { continue; }

            Set<Balloon> connectedBalloons = new HashSet<>();
            for (UUID haiId : supportHais) {
                Balloon existingBalloon = haiToBalloonMap.get(haiId);
                if (existingBalloon != null) {
                    connectedBalloons.add(existingBalloon);
                }
            }

            AABB dvBounds = BalloonStitcher.getAABB(discoveredVolume);

            //Find zombie balloons in this group
            synchronized(this.balloons) {
                for (Balloon candidate : this.balloons) {
                    if (connectedBalloons.contains(candidate)) continue; // Already found via id
                    
                    if (!candidate.getAABB().intersects(dvBounds)) continue;

                    // Check for block overlap
                    for (BlockPos p : discoveredVolume.volume()) {
                        if (candidate.contains(p)) {
                            connectedBalloons.add(candidate);
                            break;
                        }
                    }
                }
            }

            //Scan and steal zombie balloons from other groups
            List<HaiGroup> allGroups = registry.getHaiGroups();
            synchronized(allGroups) {
                for (HaiGroup otherGroup : allGroups) {
                    if (otherGroup == this) continue; 
                    if (!otherGroup.hais.isEmpty()) continue; // Only steal from Zombie Groups

                    List<Balloon> balloonsToSteal = new ArrayList<>();
                    synchronized(otherGroup.balloons) {
                        for (Balloon candidate : otherGroup.balloons) {
                            if (!candidate.getAABB().intersects(dvBounds)) continue;
                            
                            for (BlockPos p : discoveredVolume.volume()) {
                                if (candidate.contains(p)) {
                                    balloonsToSteal.add(candidate);
                                    break;
                                }
                            }
                        }
                    }
                    
                    for (Balloon stolen : balloonsToSteal) {
                        synchronized(otherGroup.balloons) {
                            otherGroup.balloons.remove(stolen);
                        }
                        
                        synchronized(this.balloons) {
                            this.balloons.add(stolen);
                        }
                        connectedBalloons.add(stolen);
                        
                        Set<UUID> oldIds = new HashSet<>(stolen.supportHais);
                        stolen.supportHais = new ManagedHaiSet(stolen, this.haiToBalloonMap, oldIds);
                    }
                }
            }

            //Handle all cases
            if (connectedBalloons.isEmpty()) {
                createBalloon(discoveredVolume.volume(), supportHais, registry);
            } else if (connectedBalloons.size() == 1) {
                Balloon targetBalloon = connectedBalloons.iterator().next();

                targetBalloon.addAll(discoveredVolume.volume());
                targetBalloon.supportHais.addAll(supportHais); 
                targetBalloon.resolveHolesAfterMerge();
            } else {
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
        if (groupAABB != null) {
            return rleVolume.isInside(pos.getX(), pos.getY(), pos.getZ(), groupAABB, ship.getShipAABB());
        }

        //Fallback for zombie groups - only ship aabb
        AABBic shipBounds = ship.getShipAABB();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (x < shipBounds.minX() - 1 || x > shipBounds.maxX() + 1 ||
            y > shipBounds.maxY() + 1 || y < shipBounds.minY() - 1 ||
            z < shipBounds.minZ() - 1 || z > shipBounds.maxZ() + 1) {
            return false;
        }

        return true;
    }

    public ServerShip getShip() {
        return ship;
    }
}