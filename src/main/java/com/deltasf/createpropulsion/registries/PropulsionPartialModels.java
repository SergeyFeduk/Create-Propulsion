package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.jozufozu.flywheel.core.PartialModel;

import net.minecraft.resources.ResourceLocation;

public class PropulsionPartialModels {
    public static final PartialModel LODESTONE_TRACKER_INDICATOR = block("lodestone_tracker_overlay");

    private static PartialModel block(String path) {
        return new PartialModel(new ResourceLocation(CreatePropulsion.ID, "block/" + path));
    }

    public static void register() {}
}