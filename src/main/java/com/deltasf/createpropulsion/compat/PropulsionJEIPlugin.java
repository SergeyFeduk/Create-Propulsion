package com.deltasf.createpropulsion.compat;

import com.deltasf.createpropulsion.CreatePropulsion;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

@JeiPlugin
public class PropulsionJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "jei_plugin");
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {}
}
