package com.deltasf.createpropulsion.balloons.registries;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.events.BalloonVolumeChangeEvent;
import com.deltasf.createpropulsion.balloons.injectors.AbstractHotAirInjectorBlockEntity;
import com.deltasf.createpropulsion.balloons.network.BalloonSyncManager;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;
import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.debug.routes.BalloonDebugRoute;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;

public class BalloonRegistry {
    public BalloonRegistry() {}
    private final AtomicInteger nextBalloonId = new AtomicInteger(0);
    public int nextBalloonId() { return nextBalloonId.getAndIncrement(); }
    private final List<Balloon> allActiveBalloons = new CopyOnWriteArrayList<>();

    public record HaiData(UUID id, BlockPos position, AABB aabb) {}
    
    private final Map<UUID, HaiData> haiDataMap = new HashMap<>();
    private final Map<UUID, HaiGroup> haiGroupMap = new HashMap<>();
    private final List<HaiGroup> haiGroups = Collections.synchronizedList(new ArrayList<>());
    private final Map<BlockPos, UUID> posToIdMap = new HashMap<>();

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
        UUID id = posToIdMap.get(pos);
        return id != null ? haiDataMap.get(id) : null;
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

        if (haiDataMap.containsKey(id)) {
            HaiGroup oldGroup = haiGroupMap.get(id);
            posToIdMap.remove(haiDataMap.get(id).position());
            if (oldGroup != null) {
                HaiData oldData = getHaiById(id);
                oldGroup.hais.remove(oldData);
                haiGroupMap.remove(id);
                if (BalloonRegistryUtility.didGroupSplit(oldGroup.hais)) {
                    handleSplitGroups(id, oldGroup, level);
                }
            }
        }

        haiDataMap.put(id, data);
        posToIdMap.put(pos, id);
        BalloonRegistryUtility.addHaiAndRegroup(data, haiGroups, haiGroupMap, level, this);
    }

    public void unregisterHai(UUID id, Level level) {
        HaiData data = haiDataMap.remove(id);
        if (data == null) return; 

        posToIdMap.remove(data.position());

        HaiGroup affectedGroup = haiGroupMap.get(id);
        if (affectedGroup == null) return;

        affectedGroup.hais.remove(data);
        haiGroupMap.remove(id);

        if (affectedGroup.hais.isEmpty()) {
            handleShrinkedGroup(id, affectedGroup, level);
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
        if (level.getBlockEntity(pos) instanceof AbstractHotAirInjectorBlockEntity) {
            registerHai(haiId, level, pos);
        } else {
            unregisterHai(haiId, level);
            return;
        }
        HaiGroup group = haiGroupMap.get(haiId);
        if (group != null) group.scan(level, this);
    }

    public void tickHaiGroups(Level level) {
        synchronized(haiGroups) {
            Iterator<HaiGroup> it = haiGroups.iterator();
            while(it.hasNext()) {
                HaiGroup group = it.next();
                group.tickBalloons(level, this);

                if (group.hais.isEmpty() && group.balloons.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    public void onBalloonAdded(Balloon balloon) {
        allActiveBalloons.add(balloon);
    }

    public void onBalloonRemoved(Balloon balloon) {
        allActiveBalloons.remove(balloon);
    }

    public List<Balloon> getBalloons() {
        return allActiveBalloons;
    }

    private void handleShrinkedGroup(UUID id, HaiGroup affectedGroup, Level level) {
        // Recalculate AABB
        affectedGroup.regenerateRLEVolume(level);

        // Revalidate all balloons
        for (Balloon balloon : affectedGroup.balloons) {
            balloon.supportHais.remove(id);
            if (!BalloonRegistryUtility.isBalloonValid(balloon, affectedGroup)) {
                balloon.isInvalid = true;
            }
        }
    }


    private void handleSplitGroups(UUID id, HaiGroup affectedGroup, Level level) {
        haiGroups.remove(affectedGroup);
        List<Balloon> orphanedBalloons = new ArrayList<>(affectedGroup.balloons);

        List<HaiGroup> newGroups = BalloonRegistryUtility.splitAndRecreateGroups(affectedGroup.hais, haiGroups, haiGroupMap, level);

        for(Balloon orphan : orphanedBalloons) {
            orphan.supportHais.remove(id);
            
            boolean adopted = false;
            for(HaiGroup potentialOwner : newGroups) {
                Set<UUID> ownerHaiIds = potentialOwner.hais.stream().map(HaiData::id).collect(Collectors.toSet());
                if (!Collections.disjoint(orphan.supportHais, ownerHaiIds)) {
                    if (BalloonRegistryUtility.isBalloonValid(orphan, potentialOwner)) {
                        potentialOwner.adoptOrphanBalloon(orphan, this);
                        adopted = true;
                    }
                    break;
                }
            }
            
            if (!adopted) {
                if (affectedGroup.getShip() != null) {
                    BalloonSyncManager.pushDestroy(affectedGroup.getShip().getId(), orphan.id);
                }
                onBalloonRemoved(orphan); 
            }
        }
    }

    //Events

    public void dispatchBalloonEvent(Balloon balloon, AABB changedBounds, BalloonVolumeChangeEvent.Type type) {
        if (balloon == null || changedBounds == null) return;
        MinecraftForge.EVENT_BUS.post(new BalloonVolumeChangeEvent(balloon, changedBounds, type, this));

        if (PropulsionDebug.isDebug(BalloonDebugRoute.EVENTS)) {
            String ident = changedBounds.toString();
            Color color;
            switch (type) {
                case CREATED:
                    color = Color.GREEN;
                    break;
                case DESTROYED:
                    color = Color.RED;
                    break;
                case EXTENDED:
                    color = Color.CYAN;
                    break;
                case MERGED:
                    color = Color.MAGENTA;
                    break;
                case SPLIT:
                    color = Color.BLACK;
                    break;
                case HOLE_CREATED:
                    color = Color.ORANGE;
                    break;
                case HOLE_REMOVED:
                    color = Color.PINK;
                    break;
                default:
                    color = Color.WHITE;
                    break;
            }
            DebugRenderer.drawBox(ident, changedBounds, color, 40);
        }
    }
}
