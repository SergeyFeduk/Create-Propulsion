package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.balloons.hot_air.BalloonAttachment;
import com.deltasf.createpropulsion.magnet.MagnetForceAttachment;
import com.deltasf.createpropulsion.propeller.PropellerAttachment;
import com.deltasf.createpropulsion.reaction_wheel.ReactionWheelAttachment;
import com.deltasf.createpropulsion.thruster.ThrusterForceAttachment;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.stress.BlockStressValues;

import com.deltasf.createpropulsion.balloons.serialization.BalloonSerializationHandler;
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
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.api.ValkyrienSkies;

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

        // VS Init
        ValkyrienSkies.api().registerAttachment(ValkyrienSkies.api().newAttachmentRegistrationBuilder(ThrusterForceAttachment.class)
                .useLegacySerializer()
                .build()
        );
        ValkyrienSkies.api().registerAttachment(ValkyrienSkies.api().newAttachmentRegistrationBuilder(BalloonAttachment.class)
                .useLegacySerializer()
                .build()
        );
        ValkyrienSkies.api().registerAttachment(ValkyrienSkies.api().newAttachmentRegistrationBuilder(MagnetForceAttachment.class)
                .useLegacySerializer()
                .build()
        );
        ValkyrienSkies.api().registerAttachment(ValkyrienSkies.api().newAttachmentRegistrationBuilder(PropellerAttachment.class)
                .useLegacySerializer()
                .build()
        );
        ValkyrienSkies.api().registerAttachment(ValkyrienSkies.api().newAttachmentRegistrationBuilder(ReactionWheelAttachment.class)
                .useLegacySerializer()
                .build()
        );

        //Compat
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CCProxy::register);
        //Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PropulsionConfig.SERVER_SPEC, ID + "-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PropulsionConfig.CLIENT_SPEC, ID + "-client.toml");
        //Registrate
        REGISTRATE.registerEventListeners(modBus);

        //TODO: Move this in correct place
        //Query ships for deserialization with balloons
        ValkyrienSkies.api().getShipLoadEvent().on((e) -> {
            //time to commit a war crime
            LoadedServerShip ship = e.getShip();
            if (ship.getAttachment(BalloonAttachment.class) != null) {
                BalloonSerializationHandler.queryShipLoad(Query.of(ship));
            }
        });

        //TODO: make stress values configurable
        BlockStressValues.IMPACTS.registerProvider(PropulsionDefaultStress::getImpact);

        modBus.addListener(CreatePropulsion::init);
    }

    public static void init(final FMLCommonSetupEvent event) {
        //TODO: Move this in correct place
        //Registers solid burner as heater
        event.enqueueWork(() -> {
            BoilerHeater.REGISTRY.register(PropulsionBlocks.SOLID_BURNER.get(), (level, pos, state) -> {
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
