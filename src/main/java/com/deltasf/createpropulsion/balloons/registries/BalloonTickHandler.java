package com.deltasf.createpropulsion.balloons.registries;

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
        for(BalloonRegistry registry : BalloonShipRegistry.get().getRegistries()) {
            registry.tickHaiGroups();
        }
    }
}
