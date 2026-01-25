package com.deltasf.createpropulsion.balloons.registries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<UUID, Integer> haiToBalloonId = new ConcurrentHashMap<>();

    public static ClientBalloon getBalloon(long shipId, int balloonId) {
        return shipBalloons.computeIfAbsent(shipId, k -> new Int2ObjectOpenHashMap<>()).get(balloonId);
    }

    public static Long2ObjectMap<Int2ObjectMap<ClientBalloon>> getAllShipBalloons() {
        return Long2ObjectMaps.unmodifiable(shipBalloons);
    }
    
    public static Int2ObjectMap<ClientBalloon> getBalloonsForShip(long shipId) {
        return shipBalloons.getOrDefault(shipId, new Int2ObjectOpenHashMap<>());
    }

    public static int getBalloonIdForHai(UUID haiId) {
        return haiToBalloonId.getOrDefault(haiId, -1);
    }

    public static void updateHaiIndex(UUID haiId, int balloonId) {
        if (balloonId == -1) {
            haiToBalloonId.remove(haiId);
        } else {
            haiToBalloonId.put(haiId, balloonId);
        }
    }

    public static void onStructurePacket(long shipId, int balloonId, long[] volume, long[] holes, UUID[] hais) {
        Int2ObjectMap<ClientBalloon> map = shipBalloons.computeIfAbsent(shipId, k -> new Int2ObjectOpenHashMap<>());
        ClientBalloon balloon = map.computeIfAbsent(balloonId, ClientBalloon::new);
        balloon.setContent(volume, holes, hais);

        ShipParticleHandler handler = BalloonParticleSystem.getHandler(shipId);
        if (handler != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                if (!balloon.volume.isEmpty()) {
                    BlockPos first = BlockPos.of(balloon.volume.iterator().nextLong());
                    handler.ensureInitialized(first.getX(), first.getY(), first.getZ());
                }
                handler.effectors.onStructureUpdate(mc.level, balloon);
            }
        }
    }

    public static void onDeltaPacket(long shipId, int balloonId, long[] added, long[] removed, long[] addedHoles, long[] removedHoles, UUID[] addedHais, UUID[] removedHais) {
        Int2ObjectMap<ClientBalloon> map = shipBalloons.computeIfAbsent(shipId, k -> new Int2ObjectOpenHashMap<>());
        ClientBalloon balloon = map.computeIfAbsent(balloonId, ClientBalloon::new);
        balloon.applyDelta(added, removed, addedHoles, removedHoles, addedHais, removedHais);

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

    public static void onUpdatePacket(long shipId, int balloonId, float hotAir) {
        Int2ObjectMap<ClientBalloon> map = shipBalloons.get(shipId);
        if (map != null) {
            ClientBalloon balloon = map.computeIfAbsent(balloonId, ClientBalloon::new);
            balloon.hotAir = hotAir;
        }
    }

    public static void onDestroyPacket(long shipId, int balloonId) {
        Int2ObjectMap<ClientBalloon> map = shipBalloons.get(shipId);
        if (map != null) {
            ClientBalloon b = map.remove(balloonId);
            if (b != null) {
                for (UUID haiId : b.connectedHais) {
                    haiToBalloonId.remove(haiId);
                }
            }
            if (map.isEmpty()) shipBalloons.remove(shipId);
        }
    }
    
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        shipBalloons.clear();
        haiToBalloonId.clear();
    }
}