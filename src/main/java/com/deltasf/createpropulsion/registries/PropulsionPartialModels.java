package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
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
    //Liquid burner
    public static final PartialModel LIQUID_BURNER_FAN = partial("liquid_burner_fan");
    //Tilt adapter
    public static final PartialModel TILT_ADAPTER_INPUT_SHAFT = partial("tilt_adapter_input_shaft");
    public static final PartialModel TILT_ADAPTER_OUTPUT_SHAFT = partial("tilt_adapter_output_shaft");
    public static final PartialModel TILT_ADAPTER_GANTRY = partial("tilt_adapter_screw_overlay");
    public static final PartialModel TILT_ADAPTER_SIDE_INDICATOR = partial("tilt_adapter_side_overlay");
    //Creative thruster
    public static final PartialModel CREATIVE_THRUSTER_BRACKET = partial("creative_thruster_bracket");
    //Transmission
    public static final PartialModel TRANSMISSION_PLUS = partial("transmission_plus");
    public static final PartialModel TRANSMISSION_MINUS = partial("transmission_minus");
    //Hot air pump
    public static final PartialModel HOT_AIR_PUMP_COG = partial("hot_air_pump_cogwheel");
    public static final PartialModel HOT_AIR_PUMP_MEMBRANE = partial("hot_air_pump_membrane");
    public static final PartialModel HOT_AIR_PUMP_MESH = partial("hot_air_pump_mesh");

    private static PartialModel partial(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "partial/" + path));
    }

    public static void register() {}
}
