package com.deltasf.createpropulsion.balloons.network;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.api.ValkyrienSkies;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.serialization.BalloonCompressor;
import com.deltasf.createpropulsion.network.PropulsionPackets;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BalloonSyncManager {

    //For periodic updates
    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 5;

    private static final Map<Long, Set<UUID>> syncedPlayers = new ConcurrentHashMap<>();
    private static final Map<Long, Set<Integer>> pendingDestroys = new ConcurrentHashMap<>();

    public static void pushDestroy(long shipId, int balloonId) {
        pendingDestroys.computeIfAbsent(shipId, k -> ConcurrentHashMap.newKeySet()).add(balloonId);
    }

    public static void requestResync(long shipId) {
        syncedPlayers.remove(shipId);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        boolean shouldSendUpdates = (tickCounter % UPDATE_INTERVAL == 0);

        var shipRegistry = BalloonShipRegistry.get();
        var shipRegistries = shipRegistry.getShipRegistries();
        var levelMap = shipRegistry.getShipToLevelMap();

        //Handle Destroys
        if (!pendingDestroys.isEmpty()) {
            for (Long shipId : pendingDestroys.keySet()) {
                ServerLevel level = levelMap.get(shipId.longValue());
                if (level == null) continue;

                Set<Integer> balloonsToKill = pendingDestroys.remove(shipId);
                if (balloonsToKill != null) {
                    for (Integer balloonId : balloonsToKill) {
                        BalloonDestroyPacket packet = new BalloonDestroyPacket(shipId, balloonId);
                        sendToWatchers(shipId, level, packet);
                    }
                }
            }
        }

        //Handle Syncing & Ticking
        for(long shipId : shipRegistries.keySet()) {
            BalloonRegistry balloonRegistry = shipRegistries.get(shipId);
            ServerLevel level = levelMap.get(shipId);
            
            if (level == null) {
                syncedPlayers.remove(shipId);
                continue;
            }

            Ship ship = ValkyrienSkies.api().getServerShipWorld().getLoadedShips().getById(shipId);
            if (!(ship instanceof LoadedServerShip loadedShip)) {
                syncedPlayers.remove(shipId);
                continue;
            }

            syncNewWatchers(shipId, loadedShip, balloonRegistry, level);

            List<ServerPlayer> currentWatchers = null;
            if (shouldSendUpdates) {
                currentWatchers = getPlayersInRange(level, loadedShip.getTransform().getPosition());
            }

            for(Balloon balloon : balloonRegistry.getBalloons()) {
                //Delta
                flushBalloon(shipId, balloon, level);

                //Updates synced every N ticks
                if (shouldSendUpdates && currentWatchers != null && !currentWatchers.isEmpty()) {
                    BalloonUpdatePacket updatePacket = new BalloonUpdatePacket(shipId, balloon.id, (float)balloon.hotAir );
                    
                    for (ServerPlayer player : currentWatchers) {
                        PropulsionPackets.sendToPlayer(updatePacket, player);
                    }
                }
            }
        }
    }

    private static void flushBalloon(long shipId, Balloon balloon, ServerLevel level) {
        Balloon.DeltaData delta = balloon.popDeltas();
        if (delta == null) return;

        try {
            byte[] ab = BalloonCompressor.compress(delta.addedBlocks());
            byte[] rb = BalloonCompressor.compress(delta.removedBlocks());
            byte[] ah = BalloonCompressor.compress(delta.addedHoles());
            byte[] rh = BalloonCompressor.compress(delta.removedHoles());

            if (delta.addedBlocks().isEmpty() && delta.removedBlocks().isEmpty() && 
                delta.addedHoles().isEmpty() && delta.removedHoles().isEmpty() &&
                delta.addedHais().isEmpty() && delta.removedHais().isEmpty()) {
                return;
            }

            // Hais
            UUID[] addedHais = delta.addedHais().toArray(new UUID[0]);
            UUID[] removedHais = delta.removedHais().toArray(new UUID[0]);

            BalloonDeltaPacket packet = new BalloonDeltaPacket(
                shipId, balloon.id,
                ab, delta.addedBlocks().size(),
                rb, delta.removedBlocks().size(),
                ah, delta.addedHoles().size(),
                rh, delta.removedHoles().size(), 
                addedHais, removedHais
            );

            sendToWatchers(shipId, level, packet);

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static void syncNewWatchers(long shipId, LoadedServerShip ship, BalloonRegistry registry, ServerLevel level) {
        List<ServerPlayer> nearbyPlayers = getPlayersInRange(level, ship.getTransform().getPosition());
        Set<UUID> syncedForThisShip = syncedPlayers.computeIfAbsent(shipId, k -> ConcurrentHashMap.newKeySet());

        List<UUID> nearbyUUIDs = nearbyPlayers.stream().map(Entity::getUUID).toList();

        syncedForThisShip.retainAll(nearbyUUIDs);

        for (ServerPlayer player : nearbyPlayers) {
            if (syncedForThisShip.add(player.getUUID())) {
                sendAllStructuresToPlayer(shipId, registry, player);
            }
        }
    }

    private static void sendAllStructuresToPlayer(long shipId, BalloonRegistry registry, ServerPlayer player) {
        List<Balloon> balloons = registry.getBalloons();
        for (Balloon balloon : balloons) {
            sendStructureToPlayer(shipId, balloon, player);
        }
    }

    private static void sendStructureToPlayer(long shipId, Balloon balloon, ServerPlayer player) {
        try {
            LongOpenHashSet volumeSet = balloon.getVolumeForSerialization();
            
            // Compress Volume
            byte[] compressedVolume = BalloonCompressor.compress(volumeSet);

            // Compress Holes
            Set<BlockPos> holes = balloon.getHoles();
            long[] holeLongs = new long[holes.size()];
            int i = 0;
            for (BlockPos pos : holes) {
                holeLongs[i++] = pos.asLong();
            }
            byte[] compressedHoles = BalloonCompressor.compress(holeLongs);

            //Hais
            UUID[] hais = balloon.getSupportHaisSet().toArray(new UUID[0]);

            // Construct Packet
            BalloonStructureSyncPacket packet = new BalloonStructureSyncPacket(
                shipId, 
                balloon.id, 
                compressedVolume, 
                volumeSet.size(), 
                compressedHoles, 
                holes.size(),
                hais
            );

            PropulsionPackets.sendToPlayer(packet, player);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void sendToWatchers(long shipId, ServerLevel level, Object packet) {
        Ship ship = ValkyrienSkies.api().getServerShipWorld().getLoadedShips().getById(shipId);
        
        if (ship instanceof LoadedServerShip loadedShip) {
            for (ServerPlayer player : getPlayersInRange(level, loadedShip.getTransform().getPosition())) {
                PropulsionPackets.sendToPlayer(packet, player);
            }
        }
    }

    public static List<ServerPlayer> getPlayersInRange(ServerLevel level, Vector3dc pos) {
        final double rangeBlocks = 32.0 * 16.0; //32x32 chunks radius
        final double rangeSq = rangeBlocks * rangeBlocks;

        return level.players().stream().filter(player -> player.distanceToSqr(pos.x(), pos.y(), pos.z()) <= rangeSq).toList();
    }
}
