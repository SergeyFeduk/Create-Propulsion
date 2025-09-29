package com.deltasf.createpropulsion.balloons.registries;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner;
import com.deltasf.createpropulsion.balloons.utils.BalloonStitcher;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner.DiscoveredVolume;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

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

    public static void tick(BalloonUpdater instance, Iterable<ServerLevel> levels) {
        //Handle all dimensions separately
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
                if (subGroup.isPlacement) {
                    handlePlacementSubGroup(subGroup);
                } else {
                    handleRemovalSubGroup(subGroup, level);
                }
            }
        }
    }

    private void handlePlacementSubGroup(EventSubGroup subGroup) {
        Set<Balloon> modifiedBalloons = new HashSet<>();
        for (Map.Entry<BlockPos, List<Balloon>> entry : subGroup.affectedBalloonsMap().entrySet()) {
            BlockPos pos = entry.getKey();
            List<Balloon> affectedBalloons = entry.getValue();

            boolean eventHandled = false;

            for (Balloon balloon : affectedBalloons) {
                if (balloon.holes.contains(pos)) {
                    BalloonStitcher.removeHole(balloon, pos);
                    modifiedBalloons.add(balloon);
                    eventHandled = true;
                }
            }

            if (eventHandled) {
                continue;
            }

            for (Balloon balloon : affectedBalloons) {
                if (balloon.contains(pos)) {
                    BalloonStitcher.handleSplit(balloon, pos, subGroup.haiGroup());
                    modifiedBalloons.add(balloon);
                    break;
                }
            }
        }

        //Resolve balloon's chunks
        for(Balloon balloon : modifiedBalloons) {
            balloon.resolveDirtyChunks();
        }
    }

    private void handleRemovalSubGroup(EventSubGroup subGroup, Level level) {
        HaiGroup haiGroup = subGroup.haiGroup();
        List<BlockPos> seeds = new ArrayList<>(subGroup.affectedBalloonsMap().keySet());

        //Collect all excluded balloons
        Set<Balloon> excludedBalloons = new HashSet<>();
        for (Collection<Balloon> bucket : subGroup.affectedBalloonsMap().values()) {
            excludedBalloons.addAll(bucket);
        }

        //Perform scan
        List<DiscoveredVolume> discoveredVolumes = BalloonScanner.scan(
            level, 
            seeds, 
            haiGroup, 
            excludedBalloons
        );

        Set<Balloon> modifiedBalloons = new HashSet<>();

        //Associate seeds with DiscoveredVolumes
        Map<BlockPos, DiscoveredVolume> blockToVolumeMap = new HashMap<>();
        for (DiscoveredVolume volume : discoveredVolumes) {
            for (BlockPos pos : volume.volume()) {
                blockToVolumeMap.put(pos, volume);
            }
        }
        //Invoke correct handlers
        Set<DiscoveredVolume> processedVolumes = new HashSet<>();
        for (BlockPos seed : seeds) {
            DiscoveredVolume resultVolume = blockToVolumeMap.get(seed);
            //Skip processed volume
            if (resultVolume == null || !processedVolumes.add(resultVolume)) {
                continue;
            }
            //Detect holes
            if (resultVolume.isLeaky()) {
                // Find all original seeds that are part of this specific leaky volume.
                for (BlockPos holeSeed : seeds) {
                    if (resultVolume.volume().contains(holeSeed)) {
                        // For each seed, get the balloons that were originally next to it.
                        List<Balloon> balloonsToGetHole = subGroup.affectedBalloonsMap().get(holeSeed);
                        for (Balloon balloon : balloonsToGetHole) {
                            //Check if the hole is NOT EXCLUSIEVELY BELOW the balloon. If so - don't create a hole as blocks below balloons volume cannot be holes
                            boolean isNotBelow = balloon.contains(holeSeed.below()) 
                                              || balloon.contains(holeSeed.north()) 
                                              || balloon.contains(holeSeed.south())
                                              || balloon.contains(holeSeed.east())
                                              || balloon.contains(holeSeed.west());
                            if (isNotBelow) {
                                BalloonStitcher.createHole(balloon, holeSeed);
                                modifiedBalloons.add(balloon);
                            }
                        }
                    }
                }
                continue; // Done with this volume.
            }
            //Handle merge/extend
            Set<Balloon> allOriginalBalloonsForThisVolume = new HashSet<>();
            for (BlockPos s : seeds) {
                if (resultVolume.volume().contains(s)) {
                    allOriginalBalloonsForThisVolume.addAll(subGroup.affectedBalloonsMap().get(s));
                }
            }
            //Find balloons overlapping with seed's volume
            Set<Balloon> overlappingBalloons = BalloonStitcher.findOverlappingBalloons(
                resultVolume,
                haiGroup,
                allOriginalBalloonsForThisVolume
            );
            //Find primary balloon and merge everything into it
            Set<Balloon> balloonsToMerge = new HashSet<>(allOriginalBalloonsForThisVolume);
            balloonsToMerge.addAll(overlappingBalloons);

            Balloon primaryBalloon = balloonsToMerge.stream()
                .max(Comparator.comparingInt(b -> b.size()))
                .orElse(null);

            if (primaryBalloon == null) continue;
            modifiedBalloons.add(primaryBalloon);

            //Extend
            BalloonStitcher.extend(primaryBalloon, resultVolume);

            //Merge
            for (Balloon otherBalloon : balloonsToMerge) {
                if (otherBalloon != primaryBalloon) {
                    BalloonStitcher.mergeInto(primaryBalloon, otherBalloon, haiGroup);
                }
            }
        }
        for (Balloon balloon : modifiedBalloons) {
            balloon.isInvalid = !BalloonRegistryUtility.isBalloonValid(balloon, haiGroup);
            balloon.resolveDirtyChunks();
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
                if (group.isPlacement() && balloon.contains(pos)) {
                    isRelevant = true;
                }
                // If not already found to be relevant, check all neighbors.
                if (!isRelevant) {
                    for (Direction dir : Direction.values()) {
                        BlockPos neighborPos = pos.relative(dir);
                        if (balloon.contains(neighborPos)) {
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
            //TODO: Actually, not true - we also need to check if we are adjacent to any hole
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
