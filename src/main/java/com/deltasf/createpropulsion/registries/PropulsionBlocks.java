package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.lodestone_tracker.LodestoneTrackerBlock;
import com.deltasf.createpropulsion.magnet.RedstoneMagnetBlock;
import com.deltasf.createpropulsion.optical_sensors.InlineOpticalSensorBlock;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlock;
import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlock;
import com.deltasf.createpropulsion.thruster.thruster.ThrusterBlock;
import com.deltasf.createpropulsion.wing.CopycatWingBlock;
import com.deltasf.createpropulsion.wing.CopycatWingItem;
import com.deltasf.createpropulsion.wing.CopycatWingModel;
import com.deltasf.createpropulsion.wing.WingBlock;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
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

    public static final BlockEntry<WingBlock> WING_BLOCK = REGISTRATE.block("wing", WingBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY))
        .properties(p -> p.strength(2.0F, 2.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();
    
    public static final BlockEntry<CopycatWingBlock> COPYCAT_WING = registerCopycatWing("copycat_wing", 4);
    public static final BlockEntry<CopycatWingBlock> COPYCAT_WING_8 = registerCopycatWing("copycat_wing_8", 8);
    public static final BlockEntry<CopycatWingBlock> COPYCAT_WING_12 = registerCopycatWing("copycat_wing_12", 12);

    private static BlockEntry<CopycatWingBlock> registerCopycatWing(String name, int width) {
        return REGISTRATE.block(name, p -> new CopycatWingBlock(p, width, () -> COPYCAT_WING.get().asItem()))
            .properties(p -> p.strength(2.0F, 2.0F))
            .transform(BuilderTransformers.copycat())
            .onRegister(CreateRegistrate.blockModel(() -> bakedModel -> new CopycatWingModel(bakedModel, width)))
            .item(CopycatWingItem::new)
            .transform(ModelGen.customItemModel("copycat_base", "wing_" + width))
            .register();
    }
}
