package com.deltasf.createpropulsion.registries;

import java.util.EnumMap;
import java.util.Map;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.envelopes.EnvelopeBlock;
import com.deltasf.createpropulsion.balloons.injectors.hot_air_burner.HotAirBurnerBlock;
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
import com.deltasf.createpropulsion.utility.PropulsionDefaultStress;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
    
    public static final BlockEntry<HotAirBurnerBlock> HOT_AIR_BURNER_BLOCK = REGISTRATE.block("hot_air_burner", HotAirBurnerBlock::new)
        .properties(p -> p.noOcclusion())
        .properties(p -> p.mapColor(MapColor.STONE))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.lightLevel(state -> state.getValue(HotAirBurnerBlock.LIT) ? 10 : 0))
        .simpleItem()
        .register();

    public static final BlockEntry<SolidBurnerBlock> SOLID_BURNER = REGISTRATE.block("solid_burner", SolidBurnerBlock::new)
        .properties(p -> p.mapColor(MapColor.STONE))
        .properties(p -> p.sound(SoundType.COPPER))
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
        .transform(PropulsionDefaultStress.setNoImpact())
        .simpleItem()
        .register();

    public static final BlockEntry<ReactionWheelBlock> REACTION_WHEEL_BLOCK = REGISTRATE.block("reaction_wheel", ReactionWheelBlock::new)
        .properties(p -> p.noOcclusion())
        .transform(PropulsionDefaultStress.setImpact(8.0))
        .simpleItem()
        .register();

    public static final BlockEntry<StirlingEngineBlock> STIRLING_ENGINE_BLOCK = REGISTRATE.block("stirling_engine", StirlingEngineBlock::new)
        .properties(p -> p.mapColor(MapColor.STONE))
        .properties(p -> p.sound(SoundType.COPPER))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.strength(2.5F, 2.0F))    
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    //All envelopes
    public enum EnvelopeColor {
        WHITE(null, MapColor.SNOW, null),
        ORANGE("orange", MapColor.COLOR_ORANGE, Items.ORANGE_DYE),
        MAGENTA("magenta", MapColor.COLOR_MAGENTA, Items.MAGENTA_DYE),
        LIGHT_BLUE("light_blue", MapColor.COLOR_LIGHT_BLUE, Items.LIGHT_BLUE_DYE),
        YELLOW("yellow", MapColor.COLOR_YELLOW, Items.YELLOW_DYE),
        LIME("lime", MapColor.COLOR_LIGHT_GREEN, Items.LIME_DYE),
        PINK("pink", MapColor.COLOR_PINK, Items.PINK_DYE),
        GRAY("gray", MapColor.COLOR_GRAY, Items.GRAY_DYE),
        LIGHT_GRAY("light_gray", MapColor.COLOR_LIGHT_GRAY, Items.LIGHT_GRAY_DYE),
        CYAN("cyan", MapColor.COLOR_CYAN, Items.CYAN_DYE),
        PURPLE("purple", MapColor.COLOR_PURPLE, Items.PURPLE_DYE),
        BLUE("blue", MapColor.COLOR_BLUE, Items.BLUE_DYE),
        BROWN("brown", MapColor.COLOR_BROWN, Items.BROWN_DYE),
        GREEN("green", MapColor.COLOR_GREEN, Items.GREEN_DYE),
        RED("red", MapColor.COLOR_RED, Items.RED_DYE),
        BLACK("black", MapColor.COLOR_BLACK, Items.BLACK_DYE);
        
        private final String name;
        private final MapColor mapColor;
        private final Item dye;

        EnvelopeColor(String name, MapColor mapColor, Item dye) {
            this.name = name;
            this.mapColor = mapColor;
            this.dye = dye;
        }
        
        public String generateId(String base) {
            if (name == null) return base;
            return base + "_" + name;
        }
        public MapColor getMapColor() { return mapColor; }
        public Item getDye() { return dye; }
    }

    private static final Map<EnvelopeColor, BlockEntry<EnvelopeBlock>> ENVELOPE_BLOCKS = new EnumMap<>(EnvelopeColor.class);
    private static final Map<EnvelopeColor, BlockEntry<EnvelopeBlock>> ENVELOPED_SHAFT_BLOCKS = new EnumMap<>(EnvelopeColor.class);
    static {
        for (EnvelopeColor color : EnvelopeColor.values()) {
            //Envelope block
            BlockEntry<EnvelopeBlock> envelope = REGISTRATE.block(color.generateId("envelope"), EnvelopeBlock::new)
                .properties(p -> p.mapColor(color.getMapColor()))
                .properties(p -> p.strength(0.5F))
                .properties(p -> p.sound(SoundType.WOOL))
                .properties(p -> p.ignitedByLava())
                .simpleItem()
                .register();
            
            ENVELOPE_BLOCKS.put(color, envelope);
            
            //Enveloped shaft block
            BlockEntry<EnvelopeBlock> envelopedShaft = REGISTRATE.block(color.generateId("enveloped_shaft"), EnvelopeBlock::new)
                .properties(p -> p.mapColor(color.getMapColor()))
                .properties(p -> p.strength(0.5F))
                .properties(p -> p.sound(SoundType.WOOL))
                .properties(p -> p.ignitedByLava())
                .simpleItem()
                .register();
            
            ENVELOPED_SHAFT_BLOCKS.put(color, envelopedShaft);
        }
    }

    public static BlockEntry<EnvelopeBlock> getEnvelope(EnvelopeColor color) {
        return ENVELOPE_BLOCKS.get(color);
    }

    public static BlockEntry<EnvelopeBlock> getEnvelopedShaft(EnvelopeColor color) {
        return ENVELOPED_SHAFT_BLOCKS.get(color);
    }
}
