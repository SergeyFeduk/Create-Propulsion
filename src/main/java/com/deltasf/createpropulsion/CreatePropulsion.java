package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PropulsionConfig.SPEC, ID + "-server.toml");
        PropulsionCreativeTab.register(modBus);
        REGISTRATE.registerEventListeners(modBus);
    }
}
