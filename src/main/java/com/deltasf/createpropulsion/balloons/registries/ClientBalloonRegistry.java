package com.deltasf.createpropulsion.balloons.registries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.deltasf.createpropulsion.balloons.ClientBalloon;
import com.deltasf.createpropulsion.balloons.particles.BalloonParticleSystem;
import com.deltasf.createpropulsion.balloons.particles.ShipParticleHandler;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientBalloonRegistry {
    private static final Map<Long, Map<Integer, ClientBalloon>> shipBalloons = new HashMap<>();

    public static ClientBalloon getBalloon(long shipId, int balloonId) {
        return shipBalloons.computeIfAbsent(shipId, k -> new HashMap<>()).get(balloonId);
    }

    public static Map<Long, Map<Integer, ClientBalloon>> getAllShipBalloons() {
        return Collections.unmodifiableMap(shipBalloons);
    }
    
    public static Map<Integer, ClientBalloon> getBalloonsForShip(long shipId) {
        return shipBalloons.getOrDefault(shipId, new HashMap<>());
    }

    public static void onStructurePacket(long shipId, int balloonId, long[] volume, long[] holes) {
        ClientBalloon balloon = shipBalloons.computeIfAbsent(shipId, k -> new HashMap<>())
                                            .computeIfAbsent(balloonId, id -> new ClientBalloon(id));
        balloon.setContent(volume, holes);

        triggerParticleUpdate(shipId, balloon);
    }

    public static void onDeltaPacket(long shipId, int balloonId, long[] added, long[] removed, long[] addedHoles, long[] removedHoles) {
        Map<Integer, ClientBalloon> map = shipBalloons.computeIfAbsent(shipId, k -> new HashMap<>());
        ClientBalloon balloon = map.computeIfAbsent(balloonId, id -> new ClientBalloon(id));
        balloon.applyDelta(added, removed, addedHoles, removedHoles);

        triggerParticleUpdate(shipId, balloon);
    }

    public static void onDestroyPacket(long shipId, int balloonId) {
        Map<Integer, ClientBalloon> map = shipBalloons.get(shipId);
        if (map != null) {
            map.remove(balloonId);
            if (map.isEmpty()) shipBalloons.remove(shipId);
        }
    }
    
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        shipBalloons.clear();
    }

    private static void triggerParticleUpdate(long shipId, ClientBalloon balloon) {
        // Only update if the handler exists and is active (near player)
        ShipParticleHandler handler = BalloonParticleSystem.getHandler(shipId);
        if (handler != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                handler.updateHoleEffectors(mc.level, balloon);
            }
        }
    }

    //TODO: This likely causes the issue with client balloon persistence
    //GC

    /*@SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Player player = mc.player;
        if (player == null) return;

        if (player.tickCount % 20 == 0) {
            pruneStaleShips();
        }
    }

    private static void pruneStaleShips() {
        var shipWorld = ValkyrienSkies.api().getClientShipWorld();
        if (shipWorld == null) return;

        Set<Long> loadedShipIds = new HashSet<>();
        for (Ship ship : shipWorld.getLoadedShips()) {
            loadedShipIds.add(ship.getId());
        }

        shipBalloons.keySet().retainAll(loadedShipIds);
    }*/
}
