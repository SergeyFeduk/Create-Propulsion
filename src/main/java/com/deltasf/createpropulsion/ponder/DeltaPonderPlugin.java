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

    //TODO: Stirling engine ponder
    // Show a solid burner, place a stirling engine on it
    // Click on solid burner with coal

    //TODO: Envelope ponder

    //TODO: Transmission ponder

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        final PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
        //Tilt adapter
        HELPER.forComponents(PropulsionBlocks.TILT_ADAPTER_BLOCK).addStoryBoard("tilt_adapter", TiltAdapterScene::tiltAdapter);
        //Burners
        HELPER.forComponents(PropulsionBlocks.SOLID_BURNER).addStoryBoard("solid_burner", BurnerScenes::solidBurner);
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