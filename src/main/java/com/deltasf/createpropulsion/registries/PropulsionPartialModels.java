package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.jozufozu.flywheel.core.PartialModel;

import net.minecraft.resources.ResourceLocation;

public class PropulsionPartialModels {
    //Lodestone
    public static final PartialModel LODESTONE_TRACKER_INDICATOR = partial("lodestone_tracker_overlay");

    //Hot air burner
    public static final PartialModel HOT_AIR_BURNER_LEVER = partial("hot_air_burner_lever");

    //Propeller
    public static final PartialModel PROPELLER_HEAD = partial("propeller_head");
    public static final PartialModel WOODEN_BLADE = partial("wooden_blade");
    public static final PartialModel COPPER_BLADE = partial("copper_blade");
    public static final PartialModel ANDESITE_BLADE = partial("andesite_blade");

    //Reaction wheel
    public static final PartialModel REACTION_WHEEL_CORE = partial("reaction_wheel_core");

    //Stirling engine
    public static final PartialModel STIRLING_ENGINE_PISTON = partial("stirling_piston");

    private static PartialModel partial(String path) {
        return new PartialModel(new ResourceLocation(CreatePropulsion.ID, "partial/" + path));
    }

    public static void register() {}
}