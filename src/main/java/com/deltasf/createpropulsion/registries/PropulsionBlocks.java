package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.blocks.EnvelopeBlock;
import com.deltasf.createpropulsion.balloons.blocks.HaiBlock;
import com.deltasf.createpropulsion.heat.burners.solid.SolidBurnerBlock;
import com.deltasf.createpropulsion.heat.engine.StirlingEngineBlock;
import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerBlock;
import com.deltasf.createpropulsion.magnet.RedstoneMagnetBlock;
import com.deltasf.createpropulsion.optical_sensors.InlineOpticalSensorBlock;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlock;
import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlock;
import com.deltasf.createpropulsion.propeller.PropellerBlock;
import com.deltasf.createpropulsion.reaction_wheel.ReactionWheelBlock;
import com.deltasf.createpropulsion.thruster.thruster.ThrusterBlock;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class PropulsionBlocks {
    public static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    public static final BlockEntry<ThrusterBlock> THRUSTER_BLOCK = REGISTRATE.block("thruster", ThrusterBlock::new)
        .properties(p -> p.mapColor(MapColor.METAL))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(5.5f, 4.0f))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    public static final BlockEntry<InlineOpticalSensorBlock> INLINE_OPTICAL_SENSOR_BLOCK = REGISTRATE.block("inline_optical_sensor", InlineOpticalSensorBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(1.5F, 1.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    public static final BlockEntry<OpticalSensorBlock> OPTICAL_SENSOR_BLOCK = REGISTRATE.block("optical_sensor", OpticalSensorBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    public static final BlockEntry<PhysicsAssemblerBlock> PHYSICS_ASSEMBLER_BLOCK = REGISTRATE.block("physics_assembler", PhysicsAssemblerBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    public static final BlockEntry<LodestoneTrackerBlock> LODESTONE_TRACKER_BLOCK = REGISTRATE.block("lodestone_tracker", LodestoneTrackerBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    public static final BlockEntry<RedstoneMagnetBlock> REDSTONE_MAGNET_BLOCK = REGISTRATE.block("redstone_magnet", RedstoneMagnetBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_RED))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();
    
    public static final BlockEntry<EnvelopeBlock> ENVELOPE_BLOCK = REGISTRATE.block("envelope", EnvelopeBlock::new)
        .properties(p -> p.mapColor(MapColor.SNOW))
        .properties(p -> p.strength(0.5F))
        .properties(p -> p.sound(SoundType.WOOL))
        .properties(p -> p.ignitedByLava())
        .simpleItem()
        .register();
    
    public static final BlockEntry<HaiBlock> HAI_BLOCK = REGISTRATE.block("hai_block", HaiBlock::new)
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    public static final BlockEntry<SolidBurnerBlock> SOLID_BURNER = REGISTRATE.block("solid_burner", SolidBurnerBlock::new)
        .properties(p -> p.mapColor(MapColor.STONE))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.lightLevel(state -> state.getValue(SolidBurnerBlock.LIT) ? 13 : 0))
        .simpleItem()
        .register();

    public static final BlockEntry<PropellerBlock> PROPELLER_BLOCK = REGISTRATE.block("propeller", PropellerBlock::new)
        .properties(p -> p.noOcclusion())
        .properties(p -> p.mapColor(MapColor.WOOD))
        .properties(p -> p.sound(SoundType.WOOD))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.strength(1.5F, 1.0F))
        .transform(BlockStressDefaults.setNoImpact())
        .simpleItem()
        .register();

    public static final BlockEntry<ReactionWheelBlock> REACTION_WHEEL_BLOCK = REGISTRATE.block("reaction_wheel", ReactionWheelBlock::new)
        .properties(p -> p.noOcclusion())    
        .transform(BlockStressDefaults.setImpact(8.0))
        .simpleItem()
        .register();

    public static final BlockEntry<StirlingEngineBlock> STIRLING_ENGINE_BLOCK = REGISTRATE.block("stirling_engine", StirlingEngineBlock::new)
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();
}
