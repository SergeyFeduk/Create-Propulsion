package com.deltasf.createpropulsion.balloons.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.balloons.utils.BalloonRegistryUtility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

public class BalloonSerializer {
    private BalloonSerializer() {}

    private static final int CURRENT_SERIALIZATION_VERSION = -1;
    private static final String MOD_DATA_FOLDER = "propulsion";
    private static final String BALLOON_DATA_FOLDER = "balloons";

    private static final int VIRTUAL_SCAN_HEIGHT = 1024; 

    public static void loadForShip(ServerLevel level, long shipId) throws IOException {
        Path filePath = getShipDataPath(level, shipId);
        if (!Files.exists(filePath)) return;

        System.out.println("[BalloonSerializer] Loading data for ship " + shipId);
        BalloonRegistry registry = BalloonShipRegistry.forShip(shipId, level);

        try (InputStream fileStream = Files.newInputStream(filePath); DataInputStream dataStream = new DataInputStream(fileStream)) {
             if (dataStream.available() < 4) return;

            int header = dataStream.readInt();

            if (header < 0 && header == -1) {
                int balloonCount = dataStream.readInt();
                System.out.println("[BalloonSerializer] Found " + balloonCount + " balloons in file.");
                for (int i = 0; i < balloonCount; i++) {
                    int length = dataStream.readInt();
                    byte[] balloonData = dataStream.readNBytes(length);
                    BalloonSerializationUtil.deserialize(balloonData, level, registry);
                }
            } else {
                int firstLength = header;
                byte[] firstBalloonData = dataStream.readNBytes(firstLength);
                BalloonSerializationUtil.deserialize(firstBalloonData, level, registry);

                while (dataStream.available() > 0) {
                    int nextLength = dataStream.readInt();
                    byte[] nextData = dataStream.readNBytes(nextLength);
                    BalloonSerializationUtil.deserialize(nextData, level, registry);
                }
            }
        }

        System.out.println("[BalloonSerializer] Starting Zombie Consolidation...");
        consolidateZombieGroups(registry);
        System.out.println("[BalloonSerializer] Consolidation Complete.");
    }

    private static void consolidateZombieGroups(BalloonRegistry registry) {
        List<HaiGroup> allGroups = registry.getHaiGroups();
        
        // Groups that are valid targets for merging (have HAIs or have saved HAI positions)
        List<HaiGroup> targetGroups = new ArrayList<>();
        // Groups that are completely isolated (no HAIs, no saved positions)
        List<HaiGroup> orphanGroups = new ArrayList<>();

        // Cache for the virtual geometry of target groups so we don't recalculate it constantly
        // Map<Group, List<ColumnAABB>>
        Map<HaiGroup, List<AABB>> virtualColumnMap = new HashMap<>();
        // Map<Group, UnionAABB>
        Map<HaiGroup, AABB> virtualBoundsMap = new HashMap<>();

        synchronized(allGroups) {
            for (HaiGroup group : allGroups) {
                Set<BlockPos> supportPositions = new HashSet<>();

                // 1. Gather all HAI positions for this group (Real + Offline)
                if (!group.hais.isEmpty()) {
                    for (HaiData data : group.hais) {
                        supportPositions.add(data.position());
                    }
                }
                
                synchronized(group.balloons) {
                    for (Balloon b : group.balloons) {
                        if (b.offlineSupportPositions != null) {
                            supportPositions.addAll(b.offlineSupportPositions);
                        }
                    }
                }

                if (!supportPositions.isEmpty()) {
                    targetGroups.add(group);
                    
                    // 2. Build Virtual Geometry
                    List<AABB> columns = new ArrayList<>();
                    AABB unionBounds = null;

                    for (BlockPos pos : supportPositions) {
                        AABB columnBox = BalloonRegistryUtility.getHaiAABB(VIRTUAL_SCAN_HEIGHT, pos);
                        columns.add(columnBox);
                        
                        if (unionBounds == null) {
                            unionBounds = columnBox;
                        } else {
                            unionBounds = unionBounds.minmax(columnBox);
                        }
                    }
                    
                    virtualColumnMap.put(group, columns);
                    virtualBoundsMap.put(group, unionBounds);

                } else {
                    orphanGroups.add(group);
                }
            }
        }

        System.out.println("[BalloonSerializer] Classification: " + orphanGroups.size() + " Orphans, " + targetGroups.size() + " Targets.");

        if (orphanGroups.isEmpty() || targetGroups.isEmpty()) return;

        // 3. Iterate Orphans to find parents
        for (int i = 0; i < orphanGroups.size(); i++) {
            HaiGroup orphanGroup = orphanGroups.get(i);
            
            // Calculate AABB for the orphan group
            AABB orphanBounds = null;
            synchronized(orphanGroup.balloons) {
                if (orphanGroup.balloons.isEmpty()) continue;
                for (Balloon b : orphanGroup.balloons) {
                    AABB bBounds = b.getAABB();
                    orphanBounds = (orphanBounds == null) ? bBounds : orphanBounds.minmax(bBounds);
                }
            }
            
            if (orphanBounds == null) continue;

            HaiGroup bestParent = null;

            // 4. Check intersection with Target Groups using Virtual Geometry
            for (HaiGroup targetGroup : targetGroups) {
                AABB targetBroadBounds = virtualBoundsMap.get(targetGroup);
                
                // Broad Phase
                if (targetBroadBounds != null && targetBroadBounds.intersects(orphanBounds)) {
                    
                    // Narrow Phase
                    boolean intersectsColumn = false;
                    List<AABB> columns = virtualColumnMap.get(targetGroup);
                    if (columns != null) {
                        for (AABB columnBox : columns) {
                            if (columnBox.intersects(orphanBounds)) {
                                intersectsColumn = true;
                                break;
                            }
                        }
                    }

                    if (intersectsColumn) {
                        bestParent = targetGroup;
                        break; 
                    }
                }
            }

            // 5. Merge if parent found
            if (bestParent != null) {
                System.out.println("[BalloonSerializer] Merging Orphan Group " + i + " into Target Group.");
                
                List<Balloon> balloonsToMove;
                synchronized(orphanGroup.balloons) {
                    balloonsToMove = new ArrayList<>(orphanGroup.balloons);
                    orphanGroup.balloons.clear(); 
                }

                for (Balloon b : balloonsToMove) {
                    // Simply move the balloon to the new group object.
                    // We DO NOT need to populate supportHais UUIDs here because we don't have them yet.
                    // When chunks load and HAIs register, 'HaiGroup.addHaiAndRegroup' will iterate the balloons 
                    // (which are now in the correct group) and link them based on position.
                    bestParent.adoptOrphanBalloon(b, registry);
                }
                
                allGroups.remove(orphanGroup);
            }
        }
    }

    public static void saveForShip(ServerLevel level, long shipId) throws IOException {
        BalloonRegistry registry = BalloonShipRegistry.forShip(shipId);
        List<Balloon> balloons = registry.getBalloons();
        
        Path filePath = getShipDataPath(level, shipId);
        if (balloons.isEmpty()) {
            Files.deleteIfExists(filePath);
            return;
        }

        Path tempFilePath = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");
        
        Files.createDirectories(filePath.getParent());

        try(OutputStream fileStream = Files.newOutputStream(tempFilePath); DataOutputStream dataStream = new DataOutputStream(fileStream)) {
            dataStream.writeInt(CURRENT_SERIALIZATION_VERSION); 
            dataStream.writeInt(balloons.size());

            for(Balloon balloon : balloons) {
                byte[] data = BalloonSerializationUtil.serialize(balloon, registry);
                dataStream.writeInt(data.length);
                dataStream.write(data);
            }
        }

        Files.move(tempFilePath, filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings("null")
    private static Path getShipDataPath(Level level, long shipId) throws IOException {
        if (level.getServer() == null) {
            throw new IOException("Cannot get save path on a client-side level.");
        }
        MinecraftServer server = level.getServer();
        Path worldSavePath = server.getWorldPath(LevelResource.ROOT);
        Path dataDir = worldSavePath.resolve(MOD_DATA_FOLDER).resolve(BALLOON_DATA_FOLDER);
        return dataDir.resolve("ship_" + shipId + ".dat");
    }
}
