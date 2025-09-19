package com.deltasf.createpropulsion;

import org.slf4j.Logger;
import org.valkyrienskies.core.impl.hooks.VSEvents;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.hooks.VSGameEvents;

import com.deltasf.createpropulsion.balloons.serialization.BalloonSerializationHandler;
import com.deltasf.createpropulsion.balloons.serialization.BalloonSerializer;
import com.deltasf.createpropulsion.balloons.serialization.BalloonSerializationHandler.Query;
import com.deltasf.createpropulsion.compat.computercraft.CCProxy;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.network.PropulsionPackets;
import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.registries.PropulsionCreativeTab;
import com.deltasf.createpropulsion.registries.PropulsionFluids;
import com.deltasf.createpropulsion.registries.PropulsionItems;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;

@Mod(CreatePropulsion.ID)
public class CreatePropulsion {
    public static final String ID = "createpropulsion";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);
    public static CreateRegistrate registrate() { return REGISTRATE; }

    public CreatePropulsion() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        //Content
        ParticleTypes.register(modBus);
        PropulsionBlocks.register();
        PropulsionBlockEntities.register();
        PropulsionItems.register();
        PropulsionFluids.register();
        PropulsionPartialModels.register();
        PropulsionCreativeTab.register(modBus);
        PropulsionPackets.register();
        //Compat
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CCProxy::register);
        //Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PropulsionConfig.SERVER_SPEC, ID + "-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PropulsionConfig.CLIENT_SPEC, ID + "-client.toml");
        //Registrate
        REGISTRATE.registerEventListeners(modBus);

        //TODO: Move this in correct place
        //Query ships for deserialization with balloons
        VSEvents.ShipLoadEvent.Companion.on((e) -> {
            BalloonSerializationHandler.queryShipLoad(new Query(e.getShip().getId(), e.getShip().getChunkClaimDimension()));
        });

        modBus.addListener(CreatePropulsion::init);

        
        
    }

    public static void init(final FMLCommonSetupEvent event) {
        //TODO: Move this in correct place
        //Registers solid burner as heater
        event.enqueueWork(() -> {
            BoilerHeaters.registerHeater(PropulsionBlocks.SOLID_BURNER.get(), (level,pos,state) -> {
                HeatLevel value = state.getValue(AbstractBurnerBlock.HEAT);
                if (value == HeatLevel.NONE) {
                    return -1;
                }
                if (value == HeatLevel.SEETHING) {
                    return 2;
                }
                if (value.isAtLeast(HeatLevel.FADING)) {
                    return 1;
                }
                return 0;
            });
        });
    }
}
