package com.deltasf.createpropulsion.balloons.utils;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.BalloonForceChunk;
import com.deltasf.createpropulsion.balloons.ClientBalloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.Balloon.ChunkKey;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.debug.routes.BalloonDebugRoute;

import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BalloonDebug {
    private static final boolean IS_ENABLED = true;
    private static final float GOLDEN_RATIO_CONJUGATE = 0.61803398875f;

    private static final Color[] GROUP_COLORS = new Color[] {
        new Color(255, 0, 0),  
        new Color(0, 255, 0),   
        new Color(0, 0, 255),   
        new Color(255, 255, 0), 
        new Color(0, 255, 255), 
        new Color(255, 0, 255)
    };

     @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!IS_ENABLED || event.phase != TickEvent.Phase.END) return;

        Map<Long, Map<Integer, ClientBalloon>> allData = ClientBalloonRegistry.getAllShipBalloons();
        if (allData.isEmpty()) return;

        for (Map.Entry<Long, Map<Integer, ClientBalloon>> entry : allData.entrySet()) {
            long shipId = entry.getKey();
            Map<Integer, ClientBalloon> shipBalloons = entry.getValue();
            for (ClientBalloon balloon : shipBalloons.values()) {
                // Use ID for stable coloring
                float hue = (balloon.id * GOLDEN_RATIO_CONJUGATE) % 1.0f;
                Color balloonColor = Color.getHSBColor(hue, 0.8f, 0.95f);

                // Render Client AABB
                if (PropulsionDebug.isDebug(BalloonDebugRoute.AABB)) {
                    renderClientAABB(shipId, balloon, balloonColor); 
                }
                // Render Client Volume
                if (PropulsionDebug.isDebug(BalloonDebugRoute.VOLUME)) {
                    renderClientVolume(balloon, balloonColor);
                }
                // Render Client Holes
                if (PropulsionDebug.isDebug(BalloonDebugRoute.HOLES)) {
                    renderClientHoles(balloon);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!IS_ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }

        BalloonShipRegistry shipRegistry = BalloonShipRegistry.get();
        if (shipRegistry == null) return;

        Collection<BalloonRegistry> allRegistries = shipRegistry.getRegistries();
        
        int groupIndex = 0;
        
        for (BalloonRegistry registry : allRegistries) {
            List<HaiGroup> groups = registry.getHaiGroups();
            if (groups.isEmpty()) continue;

            for (HaiGroup group : groups) {
                Color baseColor = GROUP_COLORS[groupIndex % GROUP_COLORS.length];
                
                if (PropulsionDebug.isDebug(BalloonDebugRoute.HAI_AABBS)) {
                    renderHaiGroupBounds(group, baseColor);
                }

                for (Balloon balloon : group.balloons) {
                    float hue = (balloon.id * GOLDEN_RATIO_CONJUGATE) % 1.0f;
                    Color balloonColor = Color.getHSBColor(hue, 0.8f, 0.95f);

                    if (PropulsionDebug.isDebug(BalloonDebugRoute.FORCE_CHUNKS)) {
                        renderBalloonForceChunks(balloon, balloonColor);
                    }
                }
                groupIndex++;
            }
        }
    }

    //Rendering functions

    private static void renderClientAABB(long shipId, ClientBalloon balloon, Color color) {
        String balloonId = "client_aabb_" + shipId + "_" + balloon.id;
        DebugRenderer.drawBox(balloonId, balloon.getBounds(), color, 1);
    }

    private static void renderClientVolume(ClientBalloon balloon, Color color) {
        String balloonIdPrefix = "client_vol_" + balloon.id + "_";
        LongIterator it = balloon.volume.iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            BlockPos pos = BlockPos.of(packed);
            String blockIdentifier = balloonIdPrefix + packed;
            DebugRenderer.drawBox(blockIdentifier, pos, color, 1);
        }
    }

    private static void renderClientHoles(ClientBalloon balloon) {
        for (BlockPos hole : balloon.holes) {
            String identifier = "client_hole_" + hole.asLong();
            DebugRenderer.drawBox(identifier, new AABB(hole), Color.white, 1);
        }
    }

    private static void renderHaiGroupBounds(HaiGroup group, Color baseColor) {
        Color haiAABBColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 80);
        for (HaiData hai : group.hais) {
            String haiIdentifier = "hai_aabb_" + hai.id().toString();
            DebugRenderer.drawBox(haiIdentifier, hai.aabb(), haiAABBColor, 3);
        }
    }

    private static void renderBalloonForceChunks(Balloon balloon, Color color) {
        if (balloon.getChunkMap().isEmpty()) {
            return; 
        }

        final int CHUNK_SIZE = Balloon.CHUNK_SIZE;

        for (Map.Entry<ChunkKey, BalloonForceChunk> entry : balloon.getChunkMap().entrySet()) {
            ChunkKey key = entry.getKey();
            BalloonForceChunk chunk = entry.getValue();

            int chunkX = key.x();
            int chunkY = key.y();
            int chunkZ = key.z();

            int originX = chunkX * CHUNK_SIZE;
            int originY = chunkY * CHUNK_SIZE;
            int originZ = chunkZ * CHUNK_SIZE;

            AABB chunkAABB = new AABB(originX, originY, originZ, originX + CHUNK_SIZE, originY + CHUNK_SIZE, originZ + CHUNK_SIZE);
            
            String chunkAABBId = "chunk_aabb_" + UUID.randomUUID(); // This UUID generation might be spammy, consider hashing pos
            Color transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60);
            DebugRenderer.drawBox(chunkAABBId, chunkAABB, transparentColor, 3);

            double chunkCenterX = originX + (CHUNK_SIZE - 1) / 2.0;
            double chunkCenterY = originY + (CHUNK_SIZE - 1) / 2.0;
            double chunkCenterZ = originZ + (CHUNK_SIZE - 1) / 2.0;

            double centroidWorldX = chunkCenterX + chunk.centroidX;
            double centroidWorldY = chunkCenterY + chunk.centroidY;
            double centroidWorldZ = chunkCenterZ + chunk.centroidZ;
            
            double halfSize = 0.25;
            AABB centroidAABB = new AABB(
                centroidWorldX - halfSize, centroidWorldY - halfSize, centroidWorldZ - halfSize,
                centroidWorldX + halfSize, centroidWorldY + halfSize, centroidWorldZ + halfSize
            );

            String centroidId = "chunk_centroid_" + UUID.randomUUID();
            DebugRenderer.drawBox(centroidId, centroidAABB, Color.WHITE, 3);
        }
    }

    //TODO: Move into the DebugRenderer class bruh
    public static void displayBlockFor(BlockPos pos, int ticks, Color color) {
        String ident = UUID.randomUUID().toString();
        DebugRenderer.drawBox(ident, pos, color, ticks);
    }
}
