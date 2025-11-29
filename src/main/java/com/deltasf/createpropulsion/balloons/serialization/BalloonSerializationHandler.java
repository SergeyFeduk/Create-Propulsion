package com.deltasf.createpropulsion.balloons.serialization;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.CreatePropulsion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BalloonSerializationHandler {
    public record Query(long id, String di) {
        public static Query of(LoadedServerShip ship) {
            return new Query(ship.getId(), ship.getChunkClaimDimension());
        }
    };
    private static final Set<Query> pendingQueries = ConcurrentHashMap.newKeySet();
    private static final Set<Long> readyShips = ConcurrentHashMap.newKeySet();

    public static void queryShipLoad(Query query) {
        pendingQueries.add(query);
    }

    public static void onHaiReady(Ship ship, Level level) {
        readyShips.add(ship.getId());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingQueries.isEmpty() || readyShips.isEmpty()) {
            return;
        }

        Iterator<Query> queryIterator = pendingQueries.iterator();
        while (queryIterator.hasNext()) {
            Query query = queryIterator.next();
            if (readyShips.contains(query.id())) {
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    if (query.di().equals(VSGameUtilsKt.getDimensionId(level))) {
                        try {
                            BalloonSerializer.loadForShip(level, query.id());
                        } catch (IOException e) {
                            System.out.println("Failed to load balloon data for ship " + query.id() + " | " +  e);
                        }
                        break;
                    }
                }
                
                queryIterator.remove();
                readyShips.remove(query.id());
            }
        }
    }
}
