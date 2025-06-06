package com.deltasf.createpropulsion.utility;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.magnet.MagnetForceAttachment;
import com.deltasf.createpropulsion.magnet.MagnetRegistry;
import com.deltasf.createpropulsion.registries.PropulsionCommands;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ThrusterFuelManager());
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        PropulsionCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
	public static void onServerStart(ServerStartedEvent event) {
        //Restore MagnetForceAttachment levels
        var levels = event.getServer().getAllLevels();
        for (ServerLevel serverLevel : levels) {
            var ships = VSGameUtilsKt.getAllShips(serverLevel);
            for (Ship ship : ships) {
                ServerShip sShip = (ServerShip)ship;
                var attachment = sShip.getAttachment(MagnetForceAttachment.class);
                if (attachment != null) {
                    attachment.level = serverLevel;
                }
            }
        }
        //Reset registry
        MagnetRegistry.get().reset();
    }
}
