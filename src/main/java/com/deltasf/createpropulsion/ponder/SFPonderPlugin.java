package com.deltasf.createpropulsion.ponder;

import org.jetbrains.annotations.NotNull;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class SFPonderPlugin implements PonderPlugin {
    @Override
    public @NotNull String getModId() {
        return CreatePropulsion.ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
        HELPER.forComponents(CreatePropulsion.THRUSTER_BLOCK).addStoryBoard("thruster_good", ThrusterPonder::ponder);
    }
}