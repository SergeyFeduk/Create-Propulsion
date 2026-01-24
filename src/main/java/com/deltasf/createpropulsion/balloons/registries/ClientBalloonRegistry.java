package com.deltasf.createpropulsion.balloons.registries;

import com.deltasf.createpropulsion.balloons.ClientBalloon;
import com.deltasf.createpropulsion.balloons.particles.BalloonParticleSystem;
import com.deltasf.createpropulsion.balloons.particles.ShipParticleHandler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientBalloonRegistry {
    private static final Long2ObjectMap<Int2ObjectMap<ClientBalloon>> shipBalloons = new Long2ObjectOpenHashMap<>();

    public static ClientBalloon getBalloon(long shipId, int balloonId) {
        return shipBalloons.computeIfAbsent(shipId, k -> new Int2ObjectOpenHashMap<>()).get(balloonId);
    }

    public static Long2ObjectMap<Int2ObjectMap<ClientBalloon>> getAllShipBalloons() {
        return Long2ObjectMaps.unmodifiable(shipBalloons);
    }
    
    public static Int2ObjectMap<ClientBalloon> getBalloonsForShip(long shipId) {
        return shipBalloons.getOrDefault(shipId, new Int2ObjectOpenHashMap<>());
    }

    public static void onStructurePacket(long shipId, int balloonId, long[] volume, long[] holes) {
        Int2ObjectMap<ClientBalloon> map = shipBalloons.computeIfAbsent(shipId, k -> new Int2ObjectOpenHashMap<>());
        ClientBalloon balloon = map.computeIfAbsent(balloonId, ClientBalloon::new);
        balloon.setContent(volume, holes);

        //Trigger full particle update
        ShipParticleHandler handler = BalloonParticleSystem.getHandler(shipId);
        if (handler != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // Initialize handler if this is the first balloon loaded so effectors can attach
                if (!balloon.volume.isEmpty()) {
                    BlockPos first = BlockPos.of(balloon.volume.iterator().nextLong());
                    handler.ensureInitialized(first.getX(), first.getY(), first.getZ());
                }
                handler.effectors.onStructureUpdate(mc.level, balloon);
            }
        }
    }

    public static void onDeltaPacket(long shipId, int balloonId, long[] added, long[] removed, long[] addedHoles, long[] removedHoles) {
        Int2ObjectMap<ClientBalloon> map = shipBalloons.computeIfAbsent(shipId, k -> new Int2ObjectOpenHashMap<>());
        ClientBalloon balloon = map.computeIfAbsent(balloonId, ClientBalloon::new);
        balloon.applyDelta(added, removed, addedHoles, removedHoles);

        //Update effectors incrementally
        if (addedHoles.length > 0 || removedHoles.length > 0) {
            ShipParticleHandler handler = BalloonParticleSystem.getHandler(shipId);
            if (handler != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    handler.effectors.onDeltaUpdate(mc.level, balloon, addedHoles, removedHoles);
                }
            }
        }
    }

    public static void onDestroyPacket(long shipId, int balloonId) {
        Int2ObjectMap<ClientBalloon> map = shipBalloons.get(shipId);
        if (map != null) {
            map.remove(balloonId);
            if (map.isEmpty()) shipBalloons.remove(shipId);
        }
    }
    
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        shipBalloons.clear();
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
