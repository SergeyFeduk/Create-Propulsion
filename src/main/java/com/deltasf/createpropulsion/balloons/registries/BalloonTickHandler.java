package com.deltasf.createpropulsion.balloons.registries;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BalloonTickHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var levels = event.getServer().getAllLevels();
        
        //Resolve balloon updates
        BalloonUpdater instance = BalloonShipRegistry.updater();
        BalloonUpdater.tick(instance, levels);
        //Tick all balloons

        Long2ObjectMap<ServerLevel> levelLookup = BalloonShipRegistry.get().getShipToLevelMap();
        for(Long2ObjectMap.Entry<BalloonRegistry> entry : BalloonShipRegistry.get().getShipRegistries().long2ObjectEntrySet()) {
            long shipid = entry.getLongKey();
            BalloonRegistry registry = entry.getValue();

            ServerLevel level = levelLookup.get(shipid);
            if (level != null) {
                registry.tickHaiGroups(level);
            }
        }
    }
}
