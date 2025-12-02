package com.deltasf.createpropulsion.registries;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.envelopes.EnvelopeBlock;
import com.deltasf.createpropulsion.balloons.envelopes.EnvelopedShaftBlock;
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
import com.deltasf.createpropulsion.wing.CopycatWingBlock;
import com.deltasf.createpropulsion.wing.CopycatWingItem;
import com.deltasf.createpropulsion.wing.CopycatWingModel;
import com.deltasf.createpropulsion.wing.WingBlock;
import com.deltasf.createpropulsion.wing.WingCTBehaviour;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;

import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;

public class PropulsionBlocks {
    public static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    //Datagen
    private static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> FUCK_OFF() {
        return (ctx, prov) -> {};
    }

    private static <I extends Item> NonNullBiConsumer<DataGenContext<Item, I>, RegistrateItemModelProvider> FUCK_OFF_ITEM() {
        return (ctx, prov) -> {};
    }

    private static <R, T extends R> NonNullBiConsumer<DataGenContext<R, T>, RegistrateLangProvider> FUCK_OFF_LANG() {
        return (ctx, prov) -> {};
    }

    public static final BlockEntry<ThrusterBlock> THRUSTER_BLOCK = REGISTRATE.block("thruster", ThrusterBlock::new)
        .properties(p -> p.mapColor(MapColor.METAL))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(5.5f, 4.0f))
        .properties(p -> p.noOcclusion())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<InlineOpticalSensorBlock> INLINE_OPTICAL_SENSOR_BLOCK = REGISTRATE.block("inline_optical_sensor", InlineOpticalSensorBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(1.5F, 1.0F))
        .properties(p -> p.noOcclusion())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<OpticalSensorBlock> OPTICAL_SENSOR_BLOCK = REGISTRATE.block("optical_sensor", OpticalSensorBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<PhysicsAssemblerBlock> PHYSICS_ASSEMBLER_BLOCK = REGISTRATE.block("physics_assembler", PhysicsAssemblerBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<LodestoneTrackerBlock> LODESTONE_TRACKER_BLOCK = REGISTRATE.block("lodestone_tracker", LodestoneTrackerBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .tag(BlockTags.MINEABLE_WITH_AXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<RedstoneMagnetBlock> REDSTONE_MAGNET_BLOCK = REGISTRATE.block("redstone_magnet", RedstoneMagnetBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_RED))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    
    public static final BlockEntry<HotAirBurnerBlock> HOT_AIR_BURNER_BLOCK = REGISTRATE.block("hot_air_burner", HotAirBurnerBlock::new)
        .properties(p -> p.noOcclusion())
        .properties(p -> p.mapColor(MapColor.STONE))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.lightLevel(state -> state.getValue(HotAirBurnerBlock.LIT) ? 10 : 0))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<SolidBurnerBlock> SOLID_BURNER = REGISTRATE.block("solid_burner", SolidBurnerBlock::new)
        .properties(p -> p.mapColor(MapColor.STONE))
        .properties(p -> p.sound(SoundType.COPPER))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.lightLevel(state -> state.getValue(SolidBurnerBlock.LIT) ? 13 : 0))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<PropellerBlock> PROPELLER_BLOCK = REGISTRATE.block("propeller", PropellerBlock::new)
        .properties(p -> p.noOcclusion())
        .properties(p -> p.mapColor(MapColor.WOOD))
        .properties(p -> p.sound(SoundType.WOOD))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.strength(1.5F, 1.0F))
        .transform(PropulsionDefaultStress.setNoImpact())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .tag(BlockTags.MINEABLE_WITH_AXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<ReactionWheelBlock> REACTION_WHEEL_BLOCK = REGISTRATE.block("reaction_wheel", ReactionWheelBlock::new)
        .properties(p -> p.noOcclusion())
        .transform(PropulsionDefaultStress.setImpact(8.0))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final BlockEntry<StirlingEngineBlock> STIRLING_ENGINE_BLOCK = REGISTRATE.block("stirling_engine", StirlingEngineBlock::new)
        .properties(p -> p.mapColor(MapColor.STONE))
        .properties(p -> p.sound(SoundType.COPPER))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.strength(2.5F, 2.0F))    
        .properties(p -> p.noOcclusion())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(FUCK_OFF())
        .item().model(FUCK_OFF_ITEM()).build()
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    //All wings
    public static final BlockEntry<WingBlock> WING_BLOCK = registerGenericWing("wing", PropulsionSpriteShifts.WING_TEXTURE);
    public static final BlockEntry<WingBlock> TEMPERED_WING_BLOCK = registerGenericWing("tempered_wing", PropulsionSpriteShifts.TEMPERED_WING_TEXTURE);
    
    public static final BlockEntry<CopycatWingBlock> COPYCAT_WING = registerCopycatWing("copycat_wing", 4);
    public static final BlockEntry<CopycatWingBlock> COPYCAT_WING_8 = registerCopycatWing("copycat_wing_8", 8);
    public static final BlockEntry<CopycatWingBlock> COPYCAT_WING_12 = registerCopycatWing("copycat_wing_12", 12);

    private static BlockEntry<WingBlock> registerGenericWing(String name, CTSpriteShiftEntry spriteShift) {
        return REGISTRATE.block(name, WingBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY))
            .properties(p -> p.sound(SoundType.COPPER))
            .properties(p -> p.strength(1.5F, 2.0F))
            .properties(p -> p.noOcclusion())
            .onRegister(connectedTextures(() -> new WingCTBehaviour(spriteShift)))
            //.addLayer(() -> RenderType::cutoutMipped)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate(FUCK_OFF())
            .item().model(FUCK_OFF_ITEM()).build()
            .setData(ProviderType.LANG, FUCK_OFF_LANG())
            .register();
    }

    private static BlockEntry<CopycatWingBlock> registerCopycatWing(String name, int width) {
        return REGISTRATE.block(name, p -> new CopycatWingBlock(p, width, () -> COPYCAT_WING.get().asItem()))
            .properties(p -> p.strength(1.5F, 2.0F))
            .transform(BuilderTransformers.copycat())
            .onRegister(CreateRegistrate.blockModel(() -> CopycatWingModel.create(width)))
            .blockstate(FUCK_OFF())
            .item(CopycatWingItem::new).build()
            .setData(ProviderType.LANG, FUCK_OFF_LANG())
            .register();
    }

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

    private static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> createEnvelopeBlockstate(String folderName, EnvelopeColor color) {
        return (ctx, prov) -> {
            String textureName = color == EnvelopeColor.WHITE ? folderName : folderName + "_" + color.name().toLowerCase();
            String texturePath = "block/" + folderName + "/" + textureName;
            ModelFile model = prov.models().cubeAll(ctx.getName(), prov.modLoc(texturePath));
            prov.simpleBlock(ctx.getEntry(), model);
        };
    }

    private static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateRecipeProvider> createDyeingRecipe(EnvelopeColor color, boolean isShaft) {
        return (ctx, prov) -> {
            if (color == EnvelopeColor.WHITE) return;

            ItemLike dyeItem = color.getDye();
            Supplier<ItemLike> baseItemSupplier = () -> isShaft 
                ? ENVELOPED_SHAFT_BLOCKS.get(EnvelopeColor.WHITE).get() 
                : ENVELOPE_BLOCKS.get(EnvelopeColor.WHITE).get();

            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ctx.getEntry())
                .requires(baseItemSupplier.get())
                .requires(dyeItem)
                .unlockedBy("has_base", RegistrateRecipeProvider.has(baseItemSupplier.get()))
                .save(prov);
        };
    }

    private static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> createEnvelopedShaftBlockstate(EnvelopeColor color) {
        return (ctx, prov) -> {
            String textureBase = "block/envelope/" + (color == EnvelopeColor.WHITE ? "envelope" : "envelope_" + color.name().toLowerCase());
            String textureShaft = "block/enveloped_shaft/" + (color == EnvelopeColor.WHITE ? "enveloped_shaft" : "enveloped_shaft_" + color.name().toLowerCase());

            BlockModelBuilder builder = prov.models()
                .withExistingParent(ctx.getName(), prov.modLoc("block/enveloped_shaft_template"))
                .texture("0", prov.modLoc(textureBase))
                .texture("1", prov.modLoc(textureShaft))
                .texture("particle", prov.modLoc(textureBase));

            prov.getVariantBuilder(ctx.getEntry())
            .forAllStates(state -> {
                Direction.Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
                if (axis == Direction.Axis.Z) {
                    return ConfiguredModel.builder().modelFile(builder).build();
                } else if (axis == Direction.Axis.X) {
                    return ConfiguredModel.builder().modelFile(builder).rotationY(90).build();
                } else {
                    return ConfiguredModel.builder().modelFile(builder).rotationX(90).build();
                }
            });

        };
    }

    public static final TagKey<Block> ENVELOPES = TagKey.create(Registries.BLOCK, new ResourceLocation(CreatePropulsion.ID, "envelopes"));

    public static final Map<EnvelopeColor, BlockEntry<EnvelopeBlock>> ENVELOPE_BLOCKS = new EnumMap<>(EnvelopeColor.class);
    public static final Map<EnvelopeColor, BlockEntry<EnvelopedShaftBlock>> ENVELOPED_SHAFT_BLOCKS = new EnumMap<>(EnvelopeColor.class);
    static {
        for (EnvelopeColor color : EnvelopeColor.values()) {
            //Envelope block
            BlockEntry<EnvelopeBlock> envelope = REGISTRATE.block(color.generateId("envelope"), EnvelopeBlock::new)
                .properties(p -> p.mapColor(color.getMapColor()))
                .properties(p -> p.strength(0.5F))
                .properties(p -> p.sound(SoundType.WOOL))
                .properties(p -> p.ignitedByLava())
                .tag(BlockTags.WOOL, ENVELOPES)
                .blockstate(createEnvelopeBlockstate("envelope", color))
                .recipe(createDyeingRecipe(color, false))
                .loot((loot, block) -> loot.add(block, loot.createSingleItemTable(block)))
                .simpleItem()
                .setData(ProviderType.LANG, FUCK_OFF_LANG())
                .register();
            
            ENVELOPE_BLOCKS.put(color, envelope);

            //Enveloped shaft block
            BlockEntry<EnvelopedShaftBlock> envelopedShaft = REGISTRATE.block(color.generateId("enveloped_shaft"), p -> new EnvelopedShaftBlock(p, envelope::get))
                .properties(p -> p.mapColor(color.getMapColor()))
                .properties(p -> p.strength(0.5F))
                .properties(p -> p.sound(SoundType.WOOL))
                .properties(p -> p.ignitedByLava())
                .properties(p -> p.noOcclusion()) 
                .transform(EncasingRegistry.addVariantTo(AllBlocks.SHAFT)) 
                .tag(BlockTags.WOOL, ENVELOPES)
                .blockstate(createEnvelopedShaftBlockstate(color))
                .loot((loot, block) -> {
                    loot.add(block, LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(envelope))
                            .when(ExplosionCondition.survivesExplosion()))
                        .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(AllBlocks.SHAFT))
                            .when(ExplosionCondition.survivesExplosion()))
                    );
                })
                .setData(ProviderType.LANG, FUCK_OFF_LANG())
                .register();
            
            ENVELOPED_SHAFT_BLOCKS.put(color, envelopedShaft);
        }
    }

    public static BlockEntry<EnvelopeBlock> getEnvelope(EnvelopeColor color) {
        return ENVELOPE_BLOCKS.get(color);
    }

    public static BlockEntry<EnvelopedShaftBlock> getEnvelopedShaft(EnvelopeColor color) {
        return ENVELOPED_SHAFT_BLOCKS.get(color);
    }
}
