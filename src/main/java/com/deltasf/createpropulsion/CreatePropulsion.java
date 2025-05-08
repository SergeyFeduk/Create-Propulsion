package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;

@Mod(CreatePropulsion.ID)
public class CreatePropulsion {
    public static final String ID = "createpropulsion";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);
    public static CreateRegistrate registrate() { return REGISTRATE; }
    
    //Compats
    public static final boolean CDG_ACTIVE = ModList.get().isLoaded("createdieselgenerators");
    public static final boolean CBC_ACTIVE = ModList.get().isLoaded("createbigcannons");
    public static final boolean TFMG_ACTIVE = ModList.get().isLoaded("tfmg");
    public static final boolean SHIMMER_ACTIVE = ModList.get().isLoaded("shimmer");

    public CreatePropulsion() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        //Content
        ParticleTypes.register(modBus);
        PropulsionBlocks.register();
        PropulsionBlockEntities.register();
        PropulsionItems.register();
        PropulsionFluids.register();
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC, ID + "-server.toml");
        PropulsionCreativeTab.register(modBus);
        REGISTRATE.registerEventListeners(modBus);
    }
}
