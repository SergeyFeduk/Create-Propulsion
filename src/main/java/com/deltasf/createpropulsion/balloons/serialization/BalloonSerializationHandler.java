package com.deltasf.createpropulsion.balloons.serialization;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private static final Queue<Query> incomingQueries = new ConcurrentLinkedQueue<>();
    
    public static void queryShipLoad(LoadedServerShip ship) {
        incomingQueries.add(Query.of(ship));
    }

    
    public static void onHaiReady(Ship ship, Level level) {
        //TODO: No-op
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || incomingQueries.isEmpty()) {
            return;
        }

        while (!incomingQueries.isEmpty()) {
            Query query = incomingQueries.poll();
            
            ServerLevel shipLevel = null;
            for (ServerLevel lvl : event.getServer().getAllLevels()) {
                if (query.di().equals(VSGameUtilsKt.getDimensionId(lvl))) {
                    shipLevel = lvl;
                    break;
                }
            }

            if (shipLevel != null) {
                try {
                    BalloonSerializer.loadForShip(shipLevel, query.id());
                    
                    com.deltasf.createpropulsion.balloons.network.BalloonSyncManager.requestResync(query.id());
                    
                } catch (IOException e) {
                    System.out.println("Failed to load balloon data for ship " + query.id() + " | " + e);
                }
            }
        }
    }
}
