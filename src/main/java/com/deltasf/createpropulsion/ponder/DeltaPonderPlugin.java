package com.deltasf.createpropulsion.ponder;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class DeltaPonderPlugin implements PonderPlugin {
    //TODO: Solid/Liquid burner ponders
    //Show a burner with stirling engine
    // - Click on a burner with coal, wait, show that stirling engine now rotates (say smth like "Heat can be used to power Stirling engines")
    // - Replace stirling engine with heated mixer ("...or heated mixers")
    // - Replace mixer with boiler ("...or boilers")
    // - Replace boiler with heat pump ("...or other heat-driven machinery")

    //TODO: Transmission ponder

    //TODO: Stirling engine ponder
    //(perhaps redirect to the Solid/Liquid burner ponder or have a separate one explaining that speed can be changed and SU output depends on heat amount)

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
        HELPER.forComponents(PropulsionBlocks.TILT_ADAPTER_BLOCK)
			.addStoryBoard("tilt_adapter", TiltAdapterScene::tiltAdapter);
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