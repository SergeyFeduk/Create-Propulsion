package com.deltasf.createpropulsion;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    //Thruster
    public static final ForgeConfigSpec.ConfigValue<Float> THRUSTER_THRUST_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Float> THRUSTER_CONSUMPTION_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Integer> THRUSTER_MAX_SPEED;
    public static final ForgeConfigSpec.ConfigValue<Integer> THRUSTER_TICKS_PER_UPDATE;
    //Optical sensors
    public static final ForgeConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_TICKS_PER_UPDATE;
    public static final ForgeConfigSpec.ConfigValue<Integer> INLINE_OPTICAL_SENSOR_MAX_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_MAX_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> OPTICAL_SENSOR_CLIP_FLUID;

    static {
        BUILDER.push("Thruster");
            THRUSTER_THRUST_MULTIPLIER = BUILDER.comment("Thrust is multiplied by that.")
                .define("Thrust multiplier", 1.0f);
            THRUSTER_CONSUMPTION_MULTIPLIER = BUILDER.comment("Fuel consumption is multiplied by that.")
                .define("Fuel consumption", 1.0f);
            THRUSTER_MAX_SPEED = BUILDER.comment("Thrusters stop accelerate ships upon reaching this speed. Defined in blocks per second.")
                .defineInRange("Thruster max speed", 100, 10, 200);
            THRUSTER_TICKS_PER_UPDATE = BUILDER.comment("Thruster tick rate. Lower values make fluid consumption a little more precise.")
                .defineInRange("Thruster tick rate", 10, 1, 100);
        BUILDER.pop();

        BUILDER.push("Optical sensors");
            OPTICAL_SENSOR_TICKS_PER_UPDATE = BUILDER.comment("How many ticks between casting a ray. Lower values are more precise, but can have negative effect on performance.")
                .defineInRange("Optical sensor tick rate", 5, 1, 100);
            INLINE_OPTICAL_SENSOR_MAX_DISTANCE = BUILDER.comment("Length of the raycast ray.")
                .defineInRange("Inline optical sensor max raycast distance", 16, 4, 32);
            OPTICAL_SENSOR_MAX_DISTANCE = BUILDER.comment("Length of the raycast ray. Very high values may degrade performance. Change with caution!")
                .defineInRange("Optical sensor max raycast distance", 64, 8, 128);
            OPTICAL_SENSOR_CLIP_FLUID = BUILDER.comment("If true - optical sensors will detect fluids too.").define("Clip fluids", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
