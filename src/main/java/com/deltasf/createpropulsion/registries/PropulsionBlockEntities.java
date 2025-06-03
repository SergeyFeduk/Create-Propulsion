package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerBlockEntity;
import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerRenderer;
import com.deltasf.createpropulsion.magnet.RedstoneMagnetBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.InlineOpticalSensorBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.rendering.OpticalSensorRenderer;
import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlockEntity;
import com.deltasf.createpropulsion.thruster.ThrusterBlockEntity;
import com.deltasf.createpropulsion.tilt_sensor.TiltSensorBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

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
        .register();
    
    public static final BlockEntityEntry<LodestoneTrackerBlockEntity> LODESTONE_TRACKER_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("lodestone_tracker_block_entity", LodestoneTrackerBlockEntity::new)
        .validBlock(PropulsionBlocks.LODESTONE_TRACKER_BLOCK)
        .renderer(() -> LodestoneTrackerRenderer::new)
        .register();

    public static final BlockEntityEntry<TiltSensorBlockEntity> TILT_SENSOR_BLOCK_ENTITY =
        REGISTRATE.blockEntity("tilt_sensor_block_entity", TiltSensorBlockEntity::new)
        .validBlock(PropulsionBlocks.TILT_SENSOR_BLOCK)
        .register();

    public static final BlockEntityEntry<RedstoneMagnetBlockEntity> REDSTONE_MAGNET_BLOCK_ENTITY =
        REGISTRATE.blockEntity("redstone_magnet_block_entity", RedstoneMagnetBlockEntity::new)
        .validBlock(PropulsionBlocks.REDSTONE_MAGNET_BLOCK)
        .register();
}
