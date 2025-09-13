package com.deltasf.createpropulsion.balloons.registries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.impl.chunk_tracking.i;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.blocks.AbstractHotAirInjectorBlockEntity;
import com.deltasf.createpropulsion.balloons.blocks.HaiBlockEntity;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class BalloonRegistry {
    public BalloonRegistry() {}
    public record HaiData(UUID id, BlockPos position, AABB aabb) {}
    
    private final Map<UUID, HaiData> haiDataMap = new HashMap<>();
    private final Map<UUID, HaiGroup> haiGroupMap = new HashMap<>();
    private final List<HaiGroup> haiGroups = Collections.synchronizedList(new ArrayList<>());

    public List<HaiGroup> getHaiGroups() {
        return haiGroups;
    }

    public HaiData getHaiById(UUID id) {
        return haiDataMap.get(id);
    }

    public int getHaiCount() {
        return haiDataMap.size();
    }

    public HaiData getHaiAt(Level level, BlockPos pos) {
        //TODO: Remove level dependency and have a map BlockPos-HaiData
        if (level.getBlockEntity(pos) instanceof AbstractHotAirInjectorBlockEntity hai) {
            return getHaiById(hai.getId());
        }
        return null;
    }

    public HaiGroup getGroupOf(UUID id) {
        return haiGroupMap.get(id);
    }

    public Balloon getBalloonOf(UUID haiId) {
        HaiGroup group = haiGroupMap.get(haiId);
        if (group == null) return null;
        return group.getBalloonFor(haiDataMap.get(haiId));
    }

    public AbstractHotAirInjectorBlockEntity getInjector(Level level, UUID id) {
        if (!haiDataMap.containsKey(id)) return null;
        BlockPos injectorPos = haiDataMap.get(id).position();

        if (level.getBlockEntity(injectorPos) instanceof AbstractHotAirInjectorBlockEntity hai) {
            return hai;
        }
        return null;
    }

    public void registerHai(UUID id, Level level, BlockPos pos) {
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        int deltaY = ship.getShipAABB().maxY() - pos.getY() - 1;

        AABB haiAabb = BalloonRegistryUtility.getHaiAABB(deltaY, pos);
        HaiData data = new HaiData(id, pos, haiAabb);

        //Update = unregister and register back 
        if (haiDataMap.containsKey(id)) {
            unregisterHai(id, level);
        }

        haiDataMap.put(id, data);
        BalloonRegistryUtility.addHaiAndRegroup(data, haiGroups, haiGroupMap, level);
    }

    public void unregisterHai(UUID id, Level level) {
        HaiData data = haiDataMap.remove(id);
        if (data == null) return; //Huh

        //Kill
        HaiGroup affectedGroup = haiGroupMap.get(id);
        if (affectedGroup == null) return;

        affectedGroup.hais.remove(data);
        haiGroupMap.remove(id);

        //The group is empty, destroy it
        if (affectedGroup.hais.isEmpty()) {
            haiGroups.remove(affectedGroup);
            haiGroupMap.remove(id);
            return;
        }

        boolean groupHasSplit = BalloonRegistryUtility.didGroupSplit(affectedGroup.hais);

        if (!groupHasSplit) {
            handleShrinkedGroup(id, affectedGroup, level);
        } else {
            handleSplitGroups(id, affectedGroup, level);
        }
    }

    public void startScanFor(UUID haiId, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof HaiBlockEntity) {
            registerHai(haiId, level, pos);
        } else {
            unregisterHai(haiId, level);
            return;
        }
        HaiGroup group = haiGroupMap.get(haiId);
        if (group != null) group.scan(level);
    }

    public void tickHaiGroups(Level level) {
        for(HaiGroup group : haiGroups) {
            group.tickBalloons(level, this);
        }
    }

    //This one should only be used for the physics thread logic as it requires syncing a bunch of stuff
    //But also can be used for serialization
    public List<Balloon> getBalloons() {
        List<Balloon> balloons = new ArrayList<>();
        synchronized (haiGroups) {
            for (HaiGroup group : haiGroups) {
                synchronized (group.balloons) {
                    balloons.addAll(group.balloons);
                }
            }
        }
        return Collections.unmodifiableList(balloons);
    }

    private void handleShrinkedGroup(UUID id, HaiGroup affectedGroup, Level level) {
        //Recalculate AABB
        affectedGroup.regenerateRLEVolume(level);

        //Revalidate all balloons
        List<Balloon> survivingBalloons = new ArrayList<>();
        for (Balloon balloon : affectedGroup.balloons) {
            balloon.supportHais.remove(id);
            if (BalloonRegistryUtility.isBalloonValid(balloon, affectedGroup)) {
                survivingBalloons.add(balloon);
            }
        }

        //Update list of group balloons
        affectedGroup.balloons.clear();
        affectedGroup.balloons.addAll(survivingBalloons);
    }

    private void handleSplitGroups(UUID id, HaiGroup affectedGroup, Level level) {
        //Original group is invalid, remove it but keep orphaned balloons
        haiGroups.remove(affectedGroup);
        List<Balloon> orphanedBalloons = new ArrayList<>(affectedGroup.balloons);

        //Create new groups from remaining pieces, add to registry and map
        List<HaiGroup> newGroups = BalloonRegistryUtility.splitAndRecreateGroups(affectedGroup.hais, haiGroups, haiGroupMap, level);

        //Try to rehome each orphaned balloon
        for(Balloon orphan : orphanedBalloons) {
            orphan.supportHais.remove(id);
            
            for(HaiGroup potentialOwner : newGroups) {
                Set<UUID> ownerHaiIds = potentialOwner.hais.stream().map(HaiData::id).collect(Collectors.toSet()); //TODO: Optimize too
                if (!Collections.disjoint(orphan.supportHais, ownerHaiIds)) {
                    //Potential home for balloon, check if it actually fits. If it does not - it is guaranteed that it is dead
                    if (BalloonRegistryUtility.isBalloonValid(orphan, potentialOwner)) {
                        potentialOwner.balloons.add(orphan);
                    }

                    break;
                }
            }
        }
    }
}
