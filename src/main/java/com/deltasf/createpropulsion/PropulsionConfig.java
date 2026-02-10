package com.deltasf.createpropulsion;

import net.minecraftforge.common.ForgeConfigSpec;

public class PropulsionConfig {
    public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    //Thruster
    public static final ForgeConfigSpec.ConfigValue<Double>  THRUSTER_THRUST_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double>  THRUSTER_CONSUMPTION_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Integer> THRUSTER_MAX_SPEED;
    public static final ForgeConfigSpec.ConfigValue<Integer> THRUSTER_TICKS_PER_UPDATE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> THRUSTER_DAMAGE_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<Double>  THRUSTER_PARTICLE_OFFSET_INCOMING_VEL_MODIFIER;
    public static final ForgeConfigSpec.ConfigValue<Double>  THRUSTER_PARTICLE_COUNT_MULTIPLIER;
    
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
    public static final ForgeConfigSpec.ConfigValue<Integer> ASSEMBLY_GAUGE_MAX_SIZE;
    
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

    public static final ForgeConfigSpec.ConfigValue<Boolean> BALLOON_PARTICLES_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Double>  BALLOON_PARTICLES_VOLUME_SPAWN_RATE;
    public static final ForgeConfigSpec.ConfigValue<Double>  BALLOON_PARTICLES_SPAWN_RADIUS;
    public static final ForgeConfigSpec.ConfigValue<Double>  BALLOON_PARTICLES_HOLE_STRENGTH;
    public static final ForgeConfigSpec.ConfigValue<Double>  BALLOON_PARTICLES_INERTIA_SCALE;
    public static final ForgeConfigSpec.ConfigValue<Integer> BALLOON_PARTICLES_ALPHA;

    //Hot air burner
    public static final ForgeConfigSpec.ConfigValue<Double> HOT_AIR_BURNER_PRODUCTION_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> HOT_AIR_BURNER_PARTICLE_SPAWN_MULTIPLIER;

    //Hot air pump
    public static final ForgeConfigSpec.ConfigValue<Double> HOT_AIR_PUMP_BASE_INJECTION_AMOUNT;
    public static final ForgeConfigSpec.ConfigValue<Double> HOT_AIR_PUMP_PARTICLE_SPAWN_MULTIPLIER;

    //Propeller
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_MAX_SPEED;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_POWER_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_WATER_POWER_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> PROPELLER_TORQUE_EFFECT_MULTIPLIER;

    public static final ForgeConfigSpec.ConfigValue<Boolean> PROPELLER_ENABLE_BLUR;
    public static final ForgeConfigSpec.ConfigValue<Integer> PROPELLER_BLUR_MAX_INSTANCES;
    public static final ForgeConfigSpec.ConfigValue<Double>  PROPELLER_BLUR_SAMPLE_RATE;
    public static final ForgeConfigSpec.ConfigValue<Double>  PROPELLER_LOD_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Double>  PROPELLER_EXPOSURE_TIME;
    public static final ForgeConfigSpec.ConfigValue<Double>  PROPELLER_BLADE_ANGLE;
    
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

    //Burners
    public static final ForgeConfigSpec.ConfigValue<Boolean> BURNERS_POWER_HEATED_MIXERS;

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

        SERVER_BUILDER.push("Creative Thruster");
            CREATIVE_THRUSTER_THRUST_MULTIPLIER = SERVER_BUILDER.comment("Thrust is multiplied by that.")
                .define("Creative thrust multiplier", 1.0);
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
            ASSEMBLY_GAUGE_MAX_SIZE = SERVER_BUILDER.comment("Maximum size of the region that can be selected with assembly gauge.")
                .define("Max size", 128);
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

            SERVER_BUILDER.push("Hot air pump");
                HOT_AIR_PUMP_BASE_INJECTION_AMOUNT = SERVER_BUILDER.comment("Base amount of hot air injected by a Hot Air Pump running at maximum speed at heat level of one (solid burner).")
                    .define("Base injection amount", 6.0);
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
                .define("Balloon force multiplier", 375.0);
            BALLOON_SURFACE_LEAK_FACTOR = SERVER_BUILDER.comment("The higher this values is - the more hot air leaks out naturally.")
                .define("Surface leak factor", 1e-2);
            BALLOON_HOLE_LEAK_FACTOR = SERVER_BUILDER.comment("The higher this values is - the more hot air leaks out of holes in balloon.")
                .define("Hole leak factor", 0.25);
            BALLOON_HOLE_LAYER_REMOVAL_THRESHOLD = SERVER_BUILDER.comment("This value controls how many holes need to be made on one balloon layer to remove it from balloon's volume.")
                .define("Hole layer removal threshold", 0.5);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Propeller");
            PROPELLER_MAX_SPEED = SERVER_BUILDER.comment("Propellers stop accelerating ships upon reaching this speed. Defined in blocks per second")
                .defineInRange("Max speed", 40.0, 10.0, 100.0);
            PROPELLER_POWER_MULTIPLIER = SERVER_BUILDER.comment("Propeller force and torque are multiplied by this number")
                .defineInRange("Power multiplier", 6.0, 0.01, 100.0);
            PROPELLER_WATER_POWER_MULTIPLIER = SERVER_BUILDER.comment("Propeller force when it is underwater is multiplied by this number")
                .defineInRange("Underwater power multiplier", 1.0, 0.01, 100.0);
            PROPELLER_TORQUE_EFFECT_MULTIPLIER = SERVER_BUILDER.comment("Propeller torque is multiplied by this number")
                .defineInRange("Torque effect multiplier", 1.0, 0.0, 100.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Stirling Engine");
            STIRLING_GENERATED_SU = SERVER_BUILDER.comment("Change this value to modify the amount of stress units produced by stirling engine. Value of 16 corresponds to 4096 SU.")
                .defineInRange("Generated stress units", 16.0, 1.0, 64.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Tilt Adapter");
            TILT_ADAPTER_ANGLE_RANGE = SERVER_BUILDER.comment("Angle range of the tilt adapter. Better leave it close to 30.")
                .defineInRange("Angle range", 30.0, 10.0, 60.0); 
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Atmosphere");
            ATMOSPHERE_HEIGHT_FACTOR = SERVER_BUILDER.comment("Height factor of the atmosphere. Increase this value if balloons are flying too low.")
                .define("Height factor", 1.0);
            ATMOSPHERE_NOISE_MAGNITUDE = SERVER_BUILDER.comment("Magnitude of the perlin noise of the atmosphere. Higher values result in higher turulence and less control over the altitude.")
                .define("Noise magnitude", 1.0);
            ATMOSPHERE_NOISE_TIME_FACTOR = SERVER_BUILDER.comment("Speed of atmospheric perlin noise change. Higher values make atmosphere (and therefore balloons and propellers) more unstable.")
                .define("Noise time factor", 1.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Burners");
            BURNERS_POWER_HEATED_MIXERS = SERVER_BUILDER.comment("If true - both solid and liquid burners can provide heat to heated mixers allowing for pre-nether brass.")
                .define("Burners power heated mixers", true);
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
                .define("Max blur instances", 64);
            PROPELLER_BLUR_SAMPLE_RATE = CLIENT_BUILDER.comment("How slow propeller blades start to become blurry")
                .define("Sample rate", 2.0);
            PROPELLER_LOD_DISTANCE = CLIENT_BUILDER.comment("Distance at which propllers no longer blur")
                .define("LOD", 128.0);
            PROPELLER_EXPOSURE_TIME = CLIENT_BUILDER.comment("Simulated exposure time. Set to 1/120 by default")
                .define("Exposure time", 1.0/120.0);
            PROPELLER_BLADE_ANGLE = CLIENT_BUILDER.comment("Angle of the propeller's blade, in degrees")
                .define("Blade angle", 10.0);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.push("Stirling Engine");
            STIRLING_REVOLUTION_PERIOD = CLIENT_BUILDER.comment("Revolution period of the simulated shaft (affects only piston movement).")
                .define("Revolution period", 0.2);
            STIRLING_CRANK_RADIUS = CLIENT_BUILDER.comment("Radius of the simulated crank.")
                .define("Crank radius", 0.125);
            STIRLING_CONROD_LENGTH = CLIENT_BUILDER.comment("Length of the simulated conrod.")
                .define("Conrod length", 0.5);
        CLIENT_BUILDER.pop();
        
        CLIENT_BUILDER.push("Hot air balloons");
            BALLOON_PARTICLES_ENABLED = CLIENT_BUILDER.comment("If true - hot air balloon particles will be spawned and rendered.")
                .define("Enable balloon particles", true);
            BALLOON_PARTICLES_VOLUME_SPAWN_RATE = CLIENT_BUILDER.comment("Multiplier for the amount of particles spawned inside the balloon volume. Higher values look better but may impact performance.")
                .define("Volume particle spawn rate", 1.0);
            BALLOON_PARTICLES_SPAWN_RADIUS = CLIENT_BUILDER.comment("Radius around the player in which balloon particles are simulated. Increasing this will reduce performance.")
                .defineInRange("Particle simulation radius", 32.0, 16.0, 128.0);
            BALLOON_PARTICLES_HOLE_STRENGTH = CLIENT_BUILDER.comment("Strength of the air jet ejecting particles from balloon holes.")
                .define("Hole eject strength", 2.8);
            BALLOON_PARTICLES_INERTIA_SCALE = CLIENT_BUILDER.comment("How much ship inertia affects particle movement. Higher values make particles lag behind the ship movement more noticeable.")
                .define("Inertia scale", 0.5);
            BALLOON_PARTICLES_ALPHA = CLIENT_BUILDER.comment("Opacity of balloon particles. 0 is invisible, 255 is fully opaque.")
                .defineInRange("Particle opacity", 100, 0, 255);
            HOT_AIR_BURNER_PARTICLE_SPAWN_MULTIPLIER = CLIENT_BUILDER.comment("Multiplier for the rate at which the Hot Air Burner spawns heat stream particles.")
                .define("Burner particle spawn multiplier", 0.1);
            HOT_AIR_PUMP_PARTICLE_SPAWN_MULTIPLIER = CLIENT_BUILDER.comment("Multiplier for the rate at which the Hot Air Pump spawns heat stream particles.")
                .define("Pump particle spawn multiplier", 0.12);

        CLIENT_BUILDER.pop();

        CLIENT_SPEC = CLIENT_BUILDER.build();
        //#endregion
    }
}
