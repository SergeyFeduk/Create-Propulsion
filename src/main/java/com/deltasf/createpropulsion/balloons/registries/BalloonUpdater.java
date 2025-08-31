package com.deltasf.createpropulsion.balloons.registries;

import java.util.List;
import java.util.Map;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.utils.BalloonDebug;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner.DiscoveredVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BalloonUpdater {
    
    private Map<ResourceKey<Level>, Queue<DynamicUpdate>> eventQueues = new HashMap<>();
    private record DynamicUpdate(BlockPos position, boolean isPlacement) {};
    private record EventGroup(List<BlockPos> positions, boolean isPlacement) {};
    private record EventSubGroup(HaiGroup haiGroup, Map<BlockPos, List<Balloon>> affectedBalloonsMap, boolean isPlacement) {};
    
    public void habBlockPlaced(BlockPos pos, Level level) {
        eventQueues.computeIfAbsent(level.dimension(), k -> new LinkedList<>())
                   .add(new DynamicUpdate(pos.immutable(), true));
    }

    public void habBlockRemoved(BlockPos pos, Level level) {
        eventQueues.computeIfAbsent(level.dimension(), k -> new LinkedList<>())
                   .add(new DynamicUpdate(pos.immutable(), false));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }
        BalloonUpdater instance = BalloonShipRegistry.updater();

        //Handle all dimensions separately
        var levels = event.getServer().getAllLevels();
        for(ServerLevel level : levels) {
            Queue<DynamicUpdate> queue = instance.eventQueues.get(level.dimension());
            if (queue != null && !queue.isEmpty()) {
                instance.resolveEvents(queue, level);
            }
        }
    }

    public void resolveEvents(Queue<DynamicUpdate> eventQueue, Level level) {
        //Phase 1: Form event groups
        List<EventGroup> eventGroups = groupEvents(eventQueue);
        //Phase 2: Resolve all elements in each group
        for(EventGroup group : eventGroups) {
            //Phase 2.1: Produce final subgroups
            List<EventSubGroup> subGroups = prefilterAndSubgroupEvents(group);
            
            //Phase 2.2: Perform scan on each subgroup
            for (EventSubGroup subGroup : subGroups) {
                //TODO: We only need to scan the removal groups, not placement ones

                HaiGroup haiGroup = subGroup.haiGroup();
                List<BlockPos> seeds = new ArrayList<>(subGroup.affectedBalloonsMap().keySet());
                //TODO: Meh, there should be an easier way with much less performance killed in the process of setting union excluded volume
                Set<BlockPos> excludedVolume = subGroup.affectedBalloonsMap().values().stream()
                    .flatMap(Collection::stream) // Get a stream of all lists of balloons
                    .distinct()                  // Get each unique balloon only once
                    .map(b -> b.volume)          // Get their volume sets
                    .flatMap(Set::stream)        // Get a stream of all BlockPos in all volumes
                    .collect(Collectors.toSet());

                List<DiscoveredVolume> discoveredVolumes = BalloonScanner.scan(
                    level, 
                    seeds, 
                    haiGroup, 
                    new ArrayList<>(excludedVolume)
                );

                for(DiscoveredVolume volume : discoveredVolumes) {
                    for(BlockPos pos : volume.volume()) {
                        BalloonDebug.displayBlockFor(pos, 100, volume.isLeaky() ? Color.red : Color.white);
                    }
                }
            }
        }
    }

    private List<EventSubGroup> prefilterAndSubgroupEvents(EventGroup group) {
        Map<HaiGroup, Map<BlockPos, List<Balloon>>> subGroupBuilders = new HashMap<>();
        //Obtain all haiGroups
        //TODO: obtain them all PER SHIP as this will reduce the amount of BalloonRegistry to 1. But note that events may occur on different ships, so its not that simple
        List<HaiGroup> allHaiGroups = BalloonShipRegistry.get().getRegistries().stream()
                .map(BalloonRegistry::getHaiGroups)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (BlockPos pos : group.positions()) {
            //Obtain haiGroup managing the given event position
            HaiGroup parentHaiGroup = null;
            for (HaiGroup haiGroup : allHaiGroups) {
                //Broad phase 
                if (haiGroup.groupAABB != null && haiGroup.groupAABB.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                    //Narrow phase
                    if (haiGroup.isInsideRleVolume(pos)) {
                        parentHaiGroup = haiGroup;
                        break; //Event can affect only one haiGroup, so early exit
                    }
                }
            }

            //Not inside haiGroup - irrelevant
            if (parentHaiGroup == null) {
                continue;
            }

            //Determine nearby balloons
            Set<Balloon> nearbyBalloons = new HashSet<>();
        
            for (Balloon balloon : parentHaiGroup.balloons) {
                boolean isRelevant = false;
                //For placement event the position may be in the volume, so we need to check for that
                if (group.isPlacement() && balloon.volume.contains(pos)) {
                    isRelevant = true;
                }
                // If not already found to be relevant, check all neighbors.
                if (!isRelevant) {
                    for (Direction dir : Direction.values()) {
                        BlockPos neighborPos = pos.relative(dir);
                        if (balloon.volume.contains(neighborPos)) {
                            isRelevant = true;
                            break;
                        }
                    }
                }
                if (isRelevant) {
                    nearbyBalloons.add(balloon);
                }
            }

            //Not adjacent to any balloon - irrelevant
            if (nearbyBalloons.isEmpty()) {
                continue;
            }

            //Event is valid, add it to subgroupBuilder
            subGroupBuilders
                .computeIfAbsent(parentHaiGroup, k -> new HashMap<>())
                .put(pos, new ArrayList<>(nearbyBalloons));
        }

        //Convert builders into final subgroups
        List<EventSubGroup> finalSubGroups = new ArrayList<>();
        for (Map.Entry<HaiGroup, Map<BlockPos, List<Balloon>>> entry : subGroupBuilders.entrySet()) {
            finalSubGroups.add(new EventSubGroup(
                entry.getKey(),
                entry.getValue(),
                group.isPlacement()
            ));
        }

        return finalSubGroups;
    }

    private List<EventGroup> groupEvents(Queue<DynamicUpdate> eventQueue) {
        List<EventGroup> eventGroups = new ArrayList<>();
        //Create first group
        DynamicUpdate firstUpdate = eventQueue.poll();
        EventGroup currentGroup = new EventGroup(new ArrayList<>(), firstUpdate.isPlacement());
        currentGroup.positions().add(firstUpdate.position());
        //Process all groups
        while(!eventQueue.isEmpty()) {
            DynamicUpdate nextUpdate = eventQueue.peek();

            if (nextUpdate.isPlacement() == currentGroup.isPlacement()) {
                eventQueue.poll(); 
                currentGroup.positions().add(nextUpdate.position());
            } else {
                eventGroups.add(currentGroup);
                DynamicUpdate newGroupStartUpdate = eventQueue.poll();
                currentGroup = new EventGroup(new ArrayList<>(), newGroupStartUpdate.isPlacement());
                currentGroup.positions().add(newGroupStartUpdate.position());
            }
        }
        //Finalize the last group
        if (!currentGroup.positions().isEmpty()) {
            eventGroups.add(currentGroup);
        }

        return eventGroups;
    }
}
