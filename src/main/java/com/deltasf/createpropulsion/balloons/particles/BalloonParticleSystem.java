package com.deltasf.createpropulsion.balloons.particles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.ClientBalloon;
import com.deltasf.createpropulsion.balloons.particles.rendering.InstancedParticleRenderer;
import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public class BalloonParticleSystem {

    private static final Long2ObjectMap<ShipParticleHandler> handlers = new Long2ObjectOpenHashMap<>();
    private static final double SPAWN_RADIUS = 32.0;
    
    // Temp
    private static final List<ClientBalloon> intersectingBalloons = new ArrayList<>();

    public static ShipParticleHandler getHandler(long shipId) {
        return handlers.get(shipId);
    }
    
    public static ShipParticleHandler getOrCreateHandler(long shipId) {
        if (handlers.containsKey(shipId)) {
            return handlers.get(shipId);
        }
        
        // Handler created for the first time
        ShipParticleHandler newHandler = new ShipParticleHandler();
        handlers.put(shipId, newHandler);
        
        // Initialize effectors for existing balloons
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Map<Integer, ClientBalloon> balloons = ClientBalloonRegistry.getBalloonsForShip(shipId);
            for (ClientBalloon balloon : balloons.values()) {
                newHandler.effectors.onStructureUpdate(mc.level, balloon);
            }
        }
        
        return newHandler;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.isPaused()) return;

        Player player = mc.player;
        if (player == null) return;
        AABB playerBounds = player.getBoundingBox().inflate(SPAWN_RADIUS); //TODO: Guh this is broken, try rotated ship
        //DebugRenderer.drawBox(player.toString(), playerBounds, 50);

        // Iterate over loaded ships
        for (ClientShip ship : VSGameUtilsKt.getShipObjectWorld(mc.level).getLoadedShips()) {
            long shipId = ship.getId();
            Int2ObjectMap<ClientBalloon> allBalloons = ClientBalloonRegistry.getBalloonsForShip(shipId);
            
            AABB shipWorldAABB = VectorConversionsMCKt.toMinecraft(ship.getRenderAABB()) ; // Approx world AABB
            if (!shipWorldAABB.intersects(playerBounds)) continue;

            // Aggregate Balloons
            intersectingBalloons.clear();
            AABB intersectionAABB = null;

            // Transform Player AABB to Ship Space for precise check
            Vector3d minW = new Vector3d(playerBounds.minX, playerBounds.minY, playerBounds.minZ);
            Vector3d maxW = new Vector3d(playerBounds.maxX, playerBounds.maxY, playerBounds.maxZ);
            ship.getTransform().getWorldToShip().transformPosition(minW);
            ship.getTransform().getWorldToShip().transformPosition(maxW);
            // Re-align AABB in ship space
            AABB playerInShip = new AABB(
                Math.min(minW.x, maxW.x), Math.min(minW.y, maxW.y), Math.min(minW.z, maxW.z),
                Math.max(minW.x, maxW.x), Math.max(minW.y, maxW.y), Math.max(minW.z, maxW.z)
            );

            for (ClientBalloon b : allBalloons.values()) {
                if (b.getBounds().intersects(playerInShip)) {
                    intersectingBalloons.add(b);
                    // Calculate intersection of balloon bounds and player bounds
                    AABB intersect = b.getBounds().intersect(playerInShip);
                    if (intersectionAABB == null) intersectionAABB = intersect;
                    else intersectionAABB = intersectionAABB.minmax(intersect);
                }
            }

            // Delegate to handler
            if (!intersectingBalloons.isEmpty() || handlers.containsKey(shipId)) {
                ShipParticleHandler handler = getOrCreateHandler(shipId);
                handler.tick(ship, allBalloons, intersectingBalloons, intersectionAABB);

                if (handler.isEmpty() && intersectingBalloons.isEmpty()) {
                    handlers.remove(shipId);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        InstancedParticleRenderer.render(
            event.getPoseStack(), 
            event.getProjectionMatrix(), 
            event.getCamera(),
            event.getPartialTick()
        );
    }
    
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        handlers.clear();

        InstancedParticleRenderer.destroy();
    }

    public static Long2ObjectMap<ShipParticleHandler> getAllHandlers() {
        return handlers;
    }
}
