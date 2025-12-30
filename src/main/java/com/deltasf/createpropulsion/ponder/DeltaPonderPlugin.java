package com.deltasf.createpropulsion.ponder;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.registries.PropulsionBlocks.EnvelopeColor;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class DeltaPonderPlugin implements PonderPlugin {
    //TODO: Stirling engine ponder
    // Show a solid burner, place a stirling engine on it
    // Click on solid burner with coal

    //TODO: Transmission ponder

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        final PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
        //Tilt adapter
        HELPER.forComponents(PropulsionBlocks.TILT_ADAPTER_BLOCK).addStoryBoard("tilt_adapter", TiltAdapterScene::tiltAdapter);
        //Burners
        HELPER.forComponents(PropulsionBlocks.SOLID_BURNER).addStoryBoard("solid_burner", BurnerScenes::solidBurner);
        HELPER.forComponents(PropulsionBlocks.LIQUID_BURNER).addStoryBoard("liquid_burner", BurnerScenes::liquidBurner);
        //Envelopes
        List<ItemProviderEntry<?>> envelopePonderables = new ArrayList<>();
        envelopePonderables.add(PropulsionBlocks.HOT_AIR_BURNER_BLOCK);
        for (EnvelopeColor color : EnvelopeColor.values()) {
            envelopePonderables.add(PropulsionBlocks.getEnvelope(color));
        }
        HELPER.forComponents(envelopePonderables).addStoryBoard("balloon", EnvelopeScenes::makingBalloon);
    }

    @Override
	public String getModId() {
		return CreatePropulsion.ID;
	}

	@Override
	public void registerScenes(@Nonnull PonderSceneRegistrationHelper<ResourceLocation> helper) {
		register(helper);
	}
}