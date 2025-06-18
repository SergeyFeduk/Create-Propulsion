package com.deltasf.createpropulsion.utility;

import java.util.HashMap;
import java.util.Map;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.magnet.MagnetForceAttachment;
import com.deltasf.createpropulsion.magnet.MagnetRegistry;
import com.deltasf.createpropulsion.registries.PropulsionCommands;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ThrusterFuelManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        PropulsionCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MagnetRegistry.get().reset();
    }

    @SubscribeEvent
	public static void onServerStart(ServerStartedEvent event) {
        //Restore MagnetForceAttachment levels
        Map<ResourceLocation, ServerLevel> levelLookup = new HashMap<>();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            levelLookup.put(level.dimension().location(), level);
        }

        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return; //We are so fucked at this stage

        final String PREFIX = "minecraft:dimension:";

        var allShips = VSGameUtilsKt.getAllShips(overworld);
        for (Ship ship : allShips) {
            if (ship instanceof ServerShip sShip) {
                var attachment = sShip.getAttachment(MagnetForceAttachment.class);
                if (attachment == null) continue;
                String shipDimensionId = sShip.getChunkClaimDimension();
                if (shipDimensionId != null && shipDimensionId.startsWith(PREFIX)) {
                    String resourceLocationString = shipDimensionId.substring(PREFIX.length());
                    ResourceLocation dimensionKey = new ResourceLocation(resourceLocationString);
                    ServerLevel level = levelLookup.get(dimensionKey);
                    if (level != null) {
                        attachment.level = level;
                    }
                }
            }
        }
    }
}
