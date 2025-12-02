package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.injectors.hot_air_burner.HotAirBurnerBlockEntity;
import com.deltasf.createpropulsion.balloons.injectors.hot_air_burner.HotAirBurnerRenderer;
import com.deltasf.createpropulsion.heat.burners.solid.SolidBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.engine.StirlingEngineBlockEntity;
import com.deltasf.createpropulsion.heat.engine.StirlingEngineRenderer;
import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerBlockEntity;
import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerRenderer;
import com.deltasf.createpropulsion.magnet.RedstoneMagnetBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.InlineOpticalSensorBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.rendering.OpticalSensorRenderer;
import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlockEntity;
import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerRenderer;
import com.deltasf.createpropulsion.propeller.PropellerBlockEntity;
import com.deltasf.createpropulsion.propeller.PropellerRenderer;
import com.deltasf.createpropulsion.reaction_wheel.ReactionWheelBlockEntity;
import com.deltasf.createpropulsion.reaction_wheel.ReactionWheelRenderer;
import com.deltasf.createpropulsion.thruster.thruster.ThrusterBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;

public class PropulsionBlockEntities {
    public static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    public static final BlockEntityEntry<ThrusterBlockEntity> THRUSTER_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("thruster_block_entity", ThrusterBlockEntity::new)
        .validBlocks(PropulsionBlocks.THRUSTER_BLOCK)
        .register();

    public static final BlockEntityEntry<InlineOpticalSensorBlockEntity> INLINE_OPTICAL_SENSOR_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("inline_optical_sensor_block_entity", InlineOpticalSensorBlockEntity::new)
        .validBlocks(PropulsionBlocks.INLINE_OPTICAL_SENSOR_BLOCK)
        .renderer(() -> OpticalSensorRenderer::new)
        .register();

    public static final BlockEntityEntry<OpticalSensorBlockEntity> OPTICAL_SENSOR_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("optical_sensor_block_entity", OpticalSensorBlockEntity::new)
        .validBlocks(PropulsionBlocks.OPTICAL_SENSOR_BLOCK)
        .renderer(() -> OpticalSensorRenderer::new)
        .register();

    public static final BlockEntityEntry<PhysicsAssemblerBlockEntity> PHYSICAL_ASSEMBLER_BLOCK_ENTITY =
        REGISTRATE.blockEntity("physics_assembler_block_entity", PhysicsAssemblerBlockEntity::new)
        .validBlock(PropulsionBlocks.PHYSICS_ASSEMBLER_BLOCK)
        .renderer(() -> PhysicsAssemblerRenderer::new)
        .register();
    
    public static final BlockEntityEntry<LodestoneTrackerBlockEntity> LODESTONE_TRACKER_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("lodestone_tracker_block_entity", LodestoneTrackerBlockEntity::new)
        .validBlock(PropulsionBlocks.LODESTONE_TRACKER_BLOCK)
        .renderer(() -> LodestoneTrackerRenderer::new)
        .register();

    public static final BlockEntityEntry<RedstoneMagnetBlockEntity> REDSTONE_MAGNET_BLOCK_ENTITY =
        REGISTRATE.blockEntity("redstone_magnet_block_entity", RedstoneMagnetBlockEntity::new)
        .validBlock(PropulsionBlocks.REDSTONE_MAGNET_BLOCK)
        .register();

    public static final BlockEntityEntry<HotAirBurnerBlockEntity> HOT_AIR_BURNER_BLOCK_ENTITY =
        REGISTRATE.blockEntity("hot_air_burner_block_entity", HotAirBurnerBlockEntity::new)
        .validBlock(PropulsionBlocks.HOT_AIR_BURNER_BLOCK)
        .renderer(() -> HotAirBurnerRenderer::new)
        .register();

    @SuppressWarnings("unchecked")
    public static final BlockEntityEntry<KineticBlockEntity> ENVELOPED_SHAFT = REGISTRATE
        .blockEntity("enveloped_shaft_block_entity", KineticBlockEntity::new)
        .renderer(() -> ShaftRenderer::new)
        .validBlocks(PropulsionBlocks.ENVELOPED_SHAFT_BLOCKS.values().toArray(new BlockEntry[0]))
        .register();

    public static final BlockEntityEntry<SolidBurnerBlockEntity> SOLID_BURNER_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("solid_burner_block_entity", SolidBurnerBlockEntity::new)
        .validBlock(PropulsionBlocks.SOLID_BURNER)
        .register();

    public static final BlockEntityEntry<PropellerBlockEntity> PROPELLER_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("propeller_block_entity", PropellerBlockEntity::new)
        //.visual(() -> PropellerRenderInstance::new)
        .validBlock(PropulsionBlocks.PROPELLER_BLOCK)
        .renderer(() -> PropellerRenderer::new)
        .register();

    public static final BlockEntityEntry<ReactionWheelBlockEntity> REACTION_WHEEL_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("reaction_wheel_block_entity", ReactionWheelBlockEntity::new)
        .validBlock(PropulsionBlocks.REACTION_WHEEL_BLOCK)
        .renderer(() -> ReactionWheelRenderer::new)
        .register();

    public static final BlockEntityEntry<StirlingEngineBlockEntity> STIRLING_ENGINE_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("stirling_engine_block_entity", StirlingEngineBlockEntity::new)
        //.instance(() -> StirlingEngineRenderInstance::new)
        .validBlock(PropulsionBlocks.STIRLING_ENGINE_BLOCK)
        .renderer(() -> StirlingEngineRenderer::new)
        .register();
}
