package com.deltasf.createpropulsion;

import net.minecraftforge.common.ForgeConfigSpec;

public class PropulsionConfig {
    public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    //Thruster
    public static final ForgeConfigSpec.ConfigValue<Double> THRUSTER_THRUST_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> THRUSTER_CONSUMPTION_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Integer> THRUSTER_MAX_SPEED;
    public static final ForgeConfigSpec.ConfigValue<Integer> THRUSTER_TICKS_PER_UPDATE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> THRUSTER_DAMAGE_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<Double> THRUSTER_PARTICLE_OFFSET_INCOMING_VEL_MODIFIER;
    public static final ForgeConfigSpec.ConfigValue<Double> THRUSTER_PARTICLE_COUNT_MULTIPLIER;
    //Optical sensors
    public static final ForgeConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_TICKS_PER_UPDATE;
    public static final ForgeConfigSpec.ConfigValue<Integer> INLINE_OPTICAL_SENSOR_MAX_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_MAX_DISTANCE;
    //Magnet
    public static final ForgeConfigSpec.ConfigValue<Double> REDSTONE_MAGNET_POWER_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> REDSTONE_MAGNET_FORCE_INDUCED_TORQUE_MULTIPLIER;
    //Physics assembler
    public static final ForgeConfigSpec.ConfigValue<Integer> PHYSICS_ASSEMBLER_MAX_MINK_DISTANCE;
    //Balloons
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_FORCE_COEFFICIENT;
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_K_COEFFICIENT;

    static {
        //#region Server
        SERVER_BUILDER.push("Thruster");
            THRUSTER_THRUST_MULTIPLIER = SERVER_BUILDER.comment("Thrust is multiplied by that.")
                .define("Thrust multiplier", 1.0);
            THRUSTER_CONSUMPTION_MULTIPLIER = SERVER_BUILDER.comment("Fuel consumption is multiplied by that.")
                .define("Fuel consumption", 1.0);
            THRUSTER_MAX_SPEED = SERVER_BUILDER.comment("Thrusters stop accelerating ships upon reaching this speed. Defined in blocks per second.")
                .defineInRange("Thruster speed limit", 100, 10, 200);
            THRUSTER_TICKS_PER_UPDATE = SERVER_BUILDER.comment("Thruster tick rate. Lower values make fluid consumption a little more precise.")
                .defineInRange("Thruster tick rate", 10, 1, 100);
            THRUSTER_DAMAGE_ENTITIES = SERVER_BUILDER.comment("If true - thrusters will damage entities. May have negative effect on performance if a lot of thrusters are used.")
                .define("Thrusters damage entities", true);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Optical sensors");
            OPTICAL_SENSOR_TICKS_PER_UPDATE = SERVER_BUILDER.comment("How many ticks between casting a ray. Lower values are more precise, but can have negative effect on performance.")
                .defineInRange("Optical sensor tick rate", 2, 1, 100);
            INLINE_OPTICAL_SENSOR_MAX_DISTANCE = SERVER_BUILDER.comment("Length of the raycast ray.")
                .defineInRange("Inline optical sensor max raycast distance", 16, 4, 32);
            OPTICAL_SENSOR_MAX_DISTANCE = SERVER_BUILDER.comment("Length of the raycast ray. Very high values may degrade performance. Change with caution!")
                .defineInRange("Optical sensor max raycast distance", 32, 8, 64);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Redstone magnet");
            REDSTONE_MAGNET_POWER_MULTIPLIER = SERVER_BUILDER.comment("Magnet power is multiplied by that.")
                .define("Power multiplier", 1.0);
            REDSTONE_MAGNET_FORCE_INDUCED_TORQUE_MULTIPLIER = SERVER_BUILDER.comment("Torque induced by offset of the force-applying magnet from COM. Value of 1.0 is realistic but does not allow for statically stable contraptions. Modify this value only if you know what you are doing!")
                .define("Force-induced torque multiplier", 1.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Physics assembler");
            PHYSICS_ASSEMBLER_MAX_MINK_DISTANCE = SERVER_BUILDER.comment("Maximum distance between region selected with assembly gauge and physics assembler block.")
                .define("Max distance to region", 3);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Physics assembler");
            BALLOON_FORCE_COEFFICIENT = SERVER_BUILDER.comment("BALLOON_FORCE_COEFFICIENT.")
                    .define("BALLOON_FORCE_COEFFICIENT", 2000.0);
            BALLOON_K_COEFFICIENT = SERVER_BUILDER.comment("BALLOON_K_COEFFICIENT.")
                    .define("BALLOON_K_COEFFICIENT", 0.3);
        SERVER_BUILDER.pop();

        SERVER_SPEC = SERVER_BUILDER.build();
        //#endregion
    
        //#region Client
        CLIENT_BUILDER.push("Thruster");
            THRUSTER_PARTICLE_OFFSET_INCOMING_VEL_MODIFIER = CLIENT_BUILDER.comment("Particle additional velocity modifier when ship is moving in the same direction as exhaust.")
                    .define("Particle velocity offset", 0.15);
            THRUSTER_PARTICLE_COUNT_MULTIPLIER = CLIENT_BUILDER.comment("The higher this number is - the more particles are spawned.")
                    .define("Particle count multiplier", 1.0);
        CLIENT_BUILDER.pop();
        CLIENT_SPEC = CLIENT_BUILDER.build();
        //#endregion
    }
}
