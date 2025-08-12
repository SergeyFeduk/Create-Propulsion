package com.deltasf.createpropulsion.events;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.registries.PropulsionItems;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    //Washing optical lenses in cauldron
    @SubscribeEvent
    public static void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CauldronInteraction.WATER.put(PropulsionItems.OPTICAL_LENS.get(), CauldronInteraction.DYED_ITEM);
        });
    }
}
