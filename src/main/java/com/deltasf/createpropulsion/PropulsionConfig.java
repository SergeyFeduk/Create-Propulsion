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
    //Creative Thruster
    public static final ForgeConfigSpec.ConfigValue<Double> CREATIVE_THRUSTER_THRUST_MULTIPLIER;
    //Optical sensors
    public static final ForgeConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_TICKS_PER_UPDATE;
    public static final ForgeConfigSpec.ConfigValue<Integer> INLINE_OPTICAL_SENSOR_MAX_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_MAX_DISTANCE;
    //Magnet
    public static final ForgeConfigSpec.ConfigValue<Double> REDSTONE_MAGNET_POWER_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> REDSTONE_MAGNET_FORCE_INDUCED_TORQUE_MULTIPLIER;
    //Physics assembler
    public static final ForgeConfigSpec.ConfigValue<Integer> PHYSICS_ASSEMBLER_MAX_MINK_DISTANCE;
    //Wings
    public static final ForgeConfigSpec.ConfigValue<Double> BASE_WING_LIFT;
    public static final ForgeConfigSpec.ConfigValue<Double> BASE_WING_DRAG;
    //Balloons
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_FORCE_COEFFICIENT;
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_ANGULAR_DAMPING;
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_ALIGNMENT_KP;
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_VERTICAL_DRAG_COEFFICIENT;
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_HORIZONTAL_DRAG_COEFFICIENT;
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_SURFACE_LEAK_FACTOR;
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_HOLE_LEAK_FACTOR;
    public static final ForgeConfigSpec.ConfigValue<Double> BALLOON_HOLE_LAYER_REMOVAL_THRESHOLD;
    //Hot air burner
    public static final ForgeConfigSpec.ConfigValue<Double> HOT_AIR_BURNER_PRODUCTION_MULTIPLIER;

    //Propeller
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_MAX_SPEED;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_POWER_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_TORQUE_EFFECT_MULTIPLIER;

    public static final ForgeConfigSpec.ConfigValue<Boolean> PROPELLER_ENABLE_BLUR;
    public static final ForgeConfigSpec.ConfigValue<Integer> PROPELLER_BLUR_MAX_INSTANCES;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_BLUR_SAMPLE_RATE;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_LOD_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_EXPOSURE_TIME;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_BLADE_ANGLE;
    //Stirling engine
    public static final ForgeConfigSpec.ConfigValue<Double> STIRLING_GENERATED_SU;

    public static final ForgeConfigSpec.ConfigValue<Double> STIRLING_REVOLUTION_PERIOD;
    public static final ForgeConfigSpec.ConfigValue<Double> STIRLING_CRANK_RADIUS;
    public static final ForgeConfigSpec.ConfigValue<Double> STIRLING_CONROD_LENGTH;
    //Tilt adapter
    public static final ForgeConfigSpec.ConfigValue<Double> TILT_ADAPTER_ANGLE_RANGE;

    //Atmosphere
    public static final ForgeConfigSpec.ConfigValue<Double> ATMOSPHERE_HEIGHT_FACTOR;
    public static final ForgeConfigSpec.ConfigValue<Double> ATMOSPHERE_NOISE_MAGNITUDE;
    public static final ForgeConfigSpec.ConfigValue<Double> ATMOSPHERE_NOISE_TIME_FACTOR;

    static {
        //#region Server
        SERVER_BUILDER.push("Thruster");
            THRUSTER_THRUST_MULTIPLIER = SERVER_BUILDER.comment("Thrust is multiplied by that.")
                .define("Thrust multiplier", 0.1);
            THRUSTER_CONSUMPTION_MULTIPLIER = SERVER_BUILDER.comment("Fuel consumption is multiplied by that.")
                .define("Fuel consumption", 1.0);
            THRUSTER_MAX_SPEED = SERVER_BUILDER.comment("Thrusters stop accelerating ships upon reaching this speed. Defined in blocks per second.")
                .defineInRange("Thruster speed limit", 100, 10, 200);
            THRUSTER_TICKS_PER_UPDATE = SERVER_BUILDER.comment("Thruster tick rate. Lower values make fluid consumption a little more precise.")
                .defineInRange("Thruster tick rate", 10, 1, 100);
            THRUSTER_DAMAGE_ENTITIES = SERVER_BUILDER.comment("If true - thrusters will damage entities. May have negative effect on performance if a lot of thrusters are used.")
                .define("Thrusters damage entities", true);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Creative Thruster");
            CREATIVE_THRUSTER_THRUST_MULTIPLIER = SERVER_BUILDER.comment("Thrust is multiplied by that.")
                .define("Creative thrust multiplier", 0.1);
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
                .define("Power multiplier", 0.1);
            REDSTONE_MAGNET_FORCE_INDUCED_TORQUE_MULTIPLIER = SERVER_BUILDER.comment("Torque induced by offset of the force-applying magnet from COM. Value of 1.0 is realistic but does not allow for statically stable contraptions. Modify this value only if you know what you are doing!")
                .define("Force-induced torque multiplier", 1.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Physics assembler");
            PHYSICS_ASSEMBLER_MAX_MINK_DISTANCE = SERVER_BUILDER.comment("Maximum distance between region selected with assembly gauge and physics assembler block.")
                .define("Max distance to region", 3);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Wing");
            BASE_WING_LIFT = SERVER_BUILDER.comment("Wing's lift force is multiplied by this number.")
                .define("Base lift", 150.0);
            BASE_WING_DRAG = SERVER_BUILDER.comment("Wing's drag force is multiplied by this number.")
                .define("Base drag", 150.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Hot air balloons");
            SERVER_BUILDER.push("Hot air burner");
                HOT_AIR_BURNER_PRODUCTION_MULTIPLIER = SERVER_BUILDER.comment("The amount of hot air generated by hot air burner is multiplied by that value.")
                    .define("Hot air burner output multiplier", 1.0);
            SERVER_BUILDER.pop();
            
            SERVER_BUILDER.push("Drag");
                BALLOON_ANGULAR_DAMPING = SERVER_BUILDER.comment("Angular damping torque is multiplied by this. Higher values slow down rotation of ships with balloons more.")
                    .define("Angular damping", 1.2);
                BALLOON_ALIGNMENT_KP = SERVER_BUILDER.comment("Vertical angular alignment torque is multiplied by this.")
                    .define("Vertical angular alignment", 10.0);
                BALLOON_VERTICAL_DRAG_COEFFICIENT = SERVER_BUILDER.comment("Vertical linear drag.")
                    .define("Vertical linear drag", 100.0);
                BALLOON_HORIZONTAL_DRAG_COEFFICIENT = SERVER_BUILDER.comment("Horizontal linear drag.")
                    .define("Horizontal linear drag", 80.0);
            SERVER_BUILDER.pop();
        
            BALLOON_FORCE_COEFFICIENT = SERVER_BUILDER.comment("Balloon's buoyant force is multiplied by that.")
                .define("Balloon force multiplier", 75.0);
            BALLOON_SURFACE_LEAK_FACTOR = SERVER_BUILDER.comment("The higher this values is - the more hot air leaks out naturally.")
                .define("Surface leak factor", 1e-2);
            BALLOON_HOLE_LEAK_FACTOR = SERVER_BUILDER.comment("The higher this values is - the more hot air leaks out of holes in balloon.")
                .define("Hole leak factor", 0.2);
            BALLOON_HOLE_LAYER_REMOVAL_THRESHOLD = SERVER_BUILDER.comment("BALLOON_HOLE_LAYER_REMOVAL_THRESHOLD")
                .define("BALLOON_HOLE_LAYER_REMOVAL_THRESHOLD", 0.5);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Propeller");
            PROPELLER_MAX_SPEED = SERVER_BUILDER.comment("Propellers stop accelerating ships upon reaching this speed. Defined in blocks per second")
                .defineInRange("Max speed", 40.0, 10.0, 100.0);
            PROPELLER_POWER_MULTIPLIER = SERVER_BUILDER.comment("Propeller force and torque are multiplied by this number")
                .defineInRange("Power multiplier", 0.8, 0.01, 100.0); //TODO: Figure out better value
            PROPELLER_TORQUE_EFFECT_MULTIPLIER = SERVER_BUILDER.comment("Propeller torque is multiplied by this number")
                .defineInRange("Torque effect multiplier", 1.0, 0.0, 100.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Stirling Engine");
            STIRLING_GENERATED_SU = SERVER_BUILDER.comment("STIRLING_GENERATED_SU")
                .defineInRange("STIRLING_GENERATED_SU", 16.0, 1.0, 64.0); //TODO: Figure out better value
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Tilt Adapter");
            TILT_ADAPTER_ANGLE_RANGE = SERVER_BUILDER.comment("TILT_ADAPTER_ANGLE_RANGE")
                .defineInRange("TILT_ADAPTER_ANGLE_RANGE", 30.0, 10.0, 60.0); 
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Atmosphere");
            ATMOSPHERE_HEIGHT_FACTOR = SERVER_BUILDER.comment("ATMOSPHERE_HEIGHT_FACTOR")
                .define("ATMOSPHERE_HEIGHT_FACTOR", 1.0);
            ATMOSPHERE_NOISE_MAGNITUDE = SERVER_BUILDER.comment("ATMOSPHERE_NOISE_MAGNITUDE")
                .define("ATMOSPHERE_NOISE_MAGNITUDE", 1.0);
            ATMOSPHERE_NOISE_TIME_FACTOR = SERVER_BUILDER.comment("ATMOSPHERE_NOISE_TIME_FACTOR")
                .define("ATMOSPHERE_NOISE_TIME_FACTOR", 1.0);
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
        CLIENT_BUILDER.push("Propeller");
            PROPELLER_ENABLE_BLUR = CLIENT_BUILDER.comment("Should fast-rotating propeller blades be blurred. Disable this if you experience visual issues with fast-rotating propellers")
                .define("Enable blur", true);
            PROPELLER_BLUR_MAX_INSTANCES = CLIENT_BUILDER.comment("Maximum amount of blurred models rendered. Decrease this value if your fps drops when near a lot of propellers.")
                .define("Max blur instances", 64); //Set to 32
            PROPELLER_BLUR_SAMPLE_RATE = CLIENT_BUILDER.comment("How slow propeller blades start to become blurry")
                .define("Sample rate", 2.0); //Set to 3
            PROPELLER_LOD_DISTANCE = CLIENT_BUILDER.comment("Distance at which propllers no longer blur")
                .define("LOD", 128.0);
            PROPELLER_EXPOSURE_TIME = CLIENT_BUILDER.comment("Simulated exposure time. Set to 1/120 by default")
                .define("Exposure time", 1.0/120.0);
            PROPELLER_BLADE_ANGLE = CLIENT_BUILDER.comment("Angle of the propeller's blade, in degrees")
                .define("Blade angle", 10.0);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.push("Stirling Engine");
            STIRLING_REVOLUTION_PERIOD = CLIENT_BUILDER.comment("STIRLING_REVOLUTION_PERIOD.")
                .define("STIRLING_REVOLUTION_PERIOD", 0.2);
            STIRLING_CRANK_RADIUS = CLIENT_BUILDER.comment("STIRLING_CRANK_RADIUS.")
                .define("STIRLING_CRANK_RADIUS", 0.125);
            STIRLING_CONROD_LENGTH = CLIENT_BUILDER.comment("STIRLING_CONROD_LENGTH.")
                .define("STIRLING_CONROD_LENGTH", 0.5);
        CLIENT_BUILDER.pop();
        CLIENT_SPEC = CLIENT_BUILDER.build();
        //#endregion
    }
}
