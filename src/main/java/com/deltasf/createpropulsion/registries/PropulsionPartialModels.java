package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.jozufozu.flywheel.core.PartialModel;

import net.minecraft.resources.ResourceLocation;

public class PropulsionPartialModels {
    //Lodestone
    public static final PartialModel LODESTONE_TRACKER_INDICATOR = block("lodestone_tracker_overlay");

    //Hot air burner
    public static final PartialModel HOT_AIR_BURNER_LEVER = block("hot_air_burner_lever");

    //Propeller
    public static final PartialModel PROPELLER_HEAD = block("propeller_head");
    public static final PartialModel WOODEN_BLADE = block("wooden_blade");
    public static final PartialModel COPPER_BLADE = block("copper_blade");
    public static final PartialModel ANDESITE_BLADE = block("andesite_blade");

    private static PartialModel block(String path) {
        return new PartialModel(new ResourceLocation(CreatePropulsion.ID, "block/" + path));
    }

    public static void register() {}
}