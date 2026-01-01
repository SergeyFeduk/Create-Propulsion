package com.deltasf.createpropulsion.balloons.registries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.deltasf.createpropulsion.balloons.ClientBalloon;

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
    }

    public static void onDeltaPacket(long shipId, int balloonId, long[] added, long[] removed, long[] addedHoles, long[] removedHoles) {
        Map<Integer, ClientBalloon> map = shipBalloons.computeIfAbsent(shipId, k -> new HashMap<>());
        ClientBalloon balloon = map.computeIfAbsent(balloonId, id -> new ClientBalloon(id));
        balloon.applyDelta(added, removed, addedHoles, removedHoles);
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
}
