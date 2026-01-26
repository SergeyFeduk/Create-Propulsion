package com.deltasf.createpropulsion.balloons.particles;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4dc;
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
    public static final double SPAWN_RADIUS = 32.0;
    public static final double SPAWN_RADIUS_SQ = SPAWN_RADIUS * SPAWN_RADIUS;
    
    // Temp
    private static final Map<ClientBalloon, AABB> perBalloonIntersections = new HashMap<>();
    private static final Vector3d tmpMin = new Vector3d();
    private static final Vector3d tmpMax = new Vector3d();

    public static ShipParticleHandler getHandler(long shipId) {
        return handlers.get(shipId);
    }
    
    public static ShipParticleHandler getOrCreateHandler(long shipId) {
        if (handlers.containsKey(shipId)) {
            return handlers.get(shipId);
        }
        
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
        AABB playerBounds = player.getBoundingBox().inflate(SPAWN_RADIUS);

        //Iterate over loaded ships
        for (ClientShip ship : VSGameUtilsKt.getShipObjectWorld(mc.level).getLoadedShips()) {
            AABB shipWorldAABB = VectorConversionsMCKt.toMinecraft(ship.getRenderAABB());
            if (!shipWorldAABB.intersects(playerBounds)) continue;

            long shipId = ship.getId();
            Int2ObjectMap<ClientBalloon> allBalloons = ClientBalloonRegistry.getBalloonsForShip(shipId);

            boolean hasBalloons = allBalloons != null && !allBalloons.isEmpty();
            boolean hasHandler = handlers.containsKey(shipId);
            
            if (allBalloons == null || (!hasBalloons && !hasHandler)) continue;

            //Aggregate balloons
            perBalloonIntersections.clear();

            if (hasBalloons) {
                Matrix4dc worldToShip = ship.getTransform().getWorldToShip();
                worldToShip.transformAab(
                    playerBounds.minX, playerBounds.minY, playerBounds.minZ,
                    playerBounds.maxX, playerBounds.maxY, playerBounds.maxZ,
                    tmpMin, tmpMax
                );

                AABB playerInShip = new AABB(tmpMin.x, tmpMin.y, tmpMin.z, tmpMax.x, tmpMax.y, tmpMax.z);

                for (ClientBalloon balloon : allBalloons.values()) {
                    if (balloon.getBounds().intersects(playerInShip)) {
                        //Calculate specific intersection
                        AABB intersect = balloon.getBounds().intersect(playerInShip);
                        perBalloonIntersections.put(balloon, intersect);
                    }
                }
            }

            //Delegate to handler
            if (!perBalloonIntersections.isEmpty() || hasHandler) {
                ShipParticleHandler handler = getOrCreateHandler(shipId);
                handler.tick(mc.level, ship, allBalloons, perBalloonIntersections);

                if (handler.isEmpty() && perBalloonIntersections.isEmpty()) {
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
