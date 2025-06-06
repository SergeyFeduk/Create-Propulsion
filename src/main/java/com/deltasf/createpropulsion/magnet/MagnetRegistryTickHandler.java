package com.deltasf.createpropulsion.magnet;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class MagnetRegistryTickHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var levels = event.getServer().getAllLevels();
        
        for(ServerLevel level : levels) {
            MagnetRegistry.get().forLevel(level).computePairs(level);
        }
    }
}
