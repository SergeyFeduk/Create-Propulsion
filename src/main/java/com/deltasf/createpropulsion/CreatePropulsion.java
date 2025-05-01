package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.optical_sensors.InlineOpticalSensorBlock;
import com.deltasf.createpropulsion.optical_sensors.InlineOpticalSensorBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlock;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.rendering.OpticalSensorRenderer;
import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlock;
import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlockEntity;
import com.deltasf.createpropulsion.thruster.ThrusterBlock;
import com.deltasf.createpropulsion.thruster.ThrusterBlockEntity;
import com.deltasf.createpropulsion.utility.BurnableItem;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.deltasf.createpropulsion.utility.TranslucentBeamRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import com.simibubi.create.foundation.item.TooltipHelper.Palette;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab.Output;

@Mod(CreatePropulsion.ID)
public class CreatePropulsion {
    public static final String ID = "createpropulsion";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);
    //Compats
    public static final boolean CDG_ACTIVE = ModList.get().isLoaded("createdieselgenerators");
    public static final boolean CBC_ACTIVE = ModList.get().isLoaded("createbigcannons");
    public static final boolean TFMG_ACTIVE = ModList.get().isLoaded("tfmg");
    public static final boolean SHIMMER_ACTIVE = ModList.get().isLoaded("shimmer");

    //Thruster
    public static final BlockEntry<ThrusterBlock> THRUSTER_BLOCK = REGISTRATE.block("thruster", ThrusterBlock::new)
        .properties(p -> p.mapColor(MapColor.METAL))
        .properties(p -> p.requiresCorrectToolForDrops())
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(5.5f, 4.0f))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    public static final BlockEntityEntry<ThrusterBlockEntity> THRUSTER_BLOCK_ENTITY = REGISTRATE.blockEntity("thruster_block_entity", ThrusterBlockEntity::new)
        .validBlocks(THRUSTER_BLOCK)
        .register();
    
    //Inline optical sensor
    public static final BlockEntry<InlineOpticalSensorBlock> INLINE_OPTICAL_SENSOR_BLOCK = REGISTRATE.block("inline_optical_sensor", InlineOpticalSensorBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(1.5F, 1.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();

    public static final BlockEntityEntry<InlineOpticalSensorBlockEntity> INLINE_OPTICAL_SENSOR_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("inline_optical_sensor_block_entity", InlineOpticalSensorBlockEntity::new)
        .validBlocks(INLINE_OPTICAL_SENSOR_BLOCK)
        .renderer(() -> OpticalSensorRenderer::new)
        .register();
    
    //Optical sensor
    public static final BlockEntry<OpticalSensorBlock> OPTICAL_SENSOR_BLOCK = REGISTRATE.block("optical_sensor", OpticalSensorBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();
    
    public static final BlockEntityEntry<OpticalSensorBlockEntity> OPTICAL_SENSOR_BLOCK_ENTITY = 
        REGISTRATE.blockEntity("optical_sensor_block_entity", OpticalSensorBlockEntity::new)
        .validBlocks(OPTICAL_SENSOR_BLOCK)
        .renderer(() -> OpticalSensorRenderer::new)
        .register();
    
    //Physics assembler
    public static final BlockEntry<PhysicsAssemblerBlock> PHYSICS_ASSEMBLER_BLOCK = REGISTRATE.block("physics_assembler", PhysicsAssemblerBlock::new)
        .properties(p -> p.mapColor(MapColor.COLOR_YELLOW))
        .properties(p -> p.sound(SoundType.METAL))
        .properties(p -> p.strength(2.5F, 2.0F))
        .properties(p -> p.noOcclusion())
        .simpleItem()
        .register();
    
    public static final BlockEntityEntry<PhysicsAssemblerBlockEntity> PHYSICAL_ASSEMBLER_BLOCK_ENTITY =
        REGISTRATE.blockEntity("physics_assembler_block_entity", PhysicsAssemblerBlockEntity::new)
        .validBlock(PHYSICS_ASSEMBLER_BLOCK)
        .register();
    
    //Turpentine and pine resin
    public static final ItemEntry<BurnableItem> PINE_RESIN = REGISTRATE.item("pine_resin", p -> new BurnableItem(p, 1200)).register();

    public static final FluidEntry<ForgeFlowingFluid.Flowing> TURPENTINE = REGISTRATE.standardFluid("turpentine", 
        SolidRenderedPlaceableFluidType.create(0xd69e49, () -> 1.0f / 4.0f, 0xffd69e49))
        .renderType(() -> RenderType.translucent())
        .lang("Turpentine")
        .properties(p -> p.viscosity(1000).density(500))
        .fluidProperties(p -> p.levelDecreasePerBlock(1)
            .tickRate(7)
            .slopeFindDistance(3)
            .explosionResistance(100f))
        .register();

    public CreatePropulsion() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ParticleTypes.register(modBus);
        
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(TranslucentBeamRenderer.class);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        REGISTRATE.registerEventListeners(modBus);

        CreativeModTab.register(modBus);
    }

    @EventBusSubscriber(modid = ID, bus = Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
            ParticleTypes.registerFactories(event);
        }
    }

    @EventBusSubscriber(bus = Bus.MOD)
    public class CreativeModTab {
        private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreatePropulsion.ID);

        public static final RegistryObject<CreativeModeTab> BASE_TAB = REGISTER.register("base", 
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.createpropulsion.base"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> THRUSTER_BLOCK.asStack())
            .displayItems(new RegistrateDisplayItemsGenerator())
            .build());

        public static void register(IEventBus modEventBus){
            REGISTER.register(modEventBus);
        }

        private static class RegistrateDisplayItemsGenerator implements DisplayItemsGenerator {
            public RegistrateDisplayItemsGenerator() {}

            @Override
            public void accept(@Nonnull ItemDisplayParameters parameters, @Nonnull Output output) {
                output.accept(INLINE_OPTICAL_SENSOR_BLOCK);
                output.accept(OPTICAL_SENSOR_BLOCK);
                output.accept(THRUSTER_BLOCK);
                output.accept(TURPENTINE.getBucket().get());
                output.accept(PINE_RESIN);
            }
        }
    }


    ///
    /// 
    /// 
    
    public static abstract class TintedFluidType extends FluidType {
		private ResourceLocation stillTexture;
		private ResourceLocation flowingTexture;

		public TintedFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
			super(properties);
			this.stillTexture = stillTexture;
			this.flowingTexture = flowingTexture;
		}

		@Override
		public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
			consumer.accept(new IClientFluidTypeExtensions() {

				@Override
				public ResourceLocation getStillTexture() {
					return stillTexture;
				}

				@Override
				public ResourceLocation getFlowingTexture() {
					return flowingTexture;
				}

				@Override
				public int getTintColor(FluidStack stack) {
					return TintedFluidType.this.getTintColor(stack);
				}

				@Override
				public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
					return TintedFluidType.this.getTintColor(state, getter, pos);
				}

				@Override
				public @NotNull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
														int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
					Vector3f customFogColor = TintedFluidType.this.getCustomFogColor();
					return customFogColor == null ? fluidFogColor : customFogColor;
				}

				@Override
				public void modifyFogRender(Camera camera, FogMode mode, float renderDistance, float partialTick,
											float nearDistance, float farDistance, FogShape shape) {
					float modifier = TintedFluidType.this.getFogDistanceModifier();
					float baseWaterFog = 96.0f;
					if (modifier != 1f) {
						RenderSystem.setShaderFogShape(FogShape.CYLINDER);
						RenderSystem.setShaderFogStart(-8);
						RenderSystem.setShaderFogEnd(baseWaterFog * modifier);
					}
				}

			});
		}

		protected abstract int getTintColor(FluidStack stack);

		protected abstract int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos);

		protected Vector3f getCustomFogColor() {
			return null;
		}

		protected float getFogDistanceModifier() {
			return 1f;
		}

	}

	private static class SolidRenderedPlaceableFluidType extends TintedFluidType {

		private Vector3f fogColor;
		private Supplier<Float> fogDistance;
        private int tintColor;

		public static FluidTypeFactory create(int fogColor, Supplier<Float> fogDistance, int tintColor) {
			return (p, s, f) -> {
                s = new ResourceLocation("block/water_still");
                f = new ResourceLocation("block/water_flow");
				SolidRenderedPlaceableFluidType fluidType = new SolidRenderedPlaceableFluidType(p, s, f);
				fluidType.fogColor = new Color(fogColor, false).asVectorF();
				fluidType.fogDistance = fogDistance;
                fluidType.tintColor = tintColor;
				return fluidType;
			};
		}

		private SolidRenderedPlaceableFluidType(Properties properties, ResourceLocation stillTexture,
												ResourceLocation flowingTexture) {
			super(properties, stillTexture, flowingTexture);
		}

		@Override
		protected int getTintColor(FluidStack stack) {
            return tintColor;
		}

		@Override
		public int getTintColor(FluidState state, BlockAndTintGetter world, BlockPos pos) {
            return tintColor;
		}

		@Override
		protected Vector3f getCustomFogColor() {
			return fogColor;
		}

		@Override
		protected float getFogDistanceModifier() {
			return fogDistance.get();
		}

	}

    ///
    /// 
    /// 

    @EventBusSubscriber(modid = ID, value = Dist.CLIENT)
    public class TooltipHandler {
        @SubscribeEvent
        public static void addToItemTooltip(ItemTooltipEvent event) {
            //Looked this up in CDG
            Item item = event.getItemStack().getItem();
            //Skip all items not from this mod
            if(ForgeRegistries.ITEMS.getKey(item).getNamespace() != ID)
                return;
            String path = ID + "." + ForgeRegistries.ITEMS.getKey(item).getPath();

            List<Component> tooltip = event.getToolTip();
            //Add Create "Hold Shift for summary"
            List<Component> tooltipList = new ArrayList<>();
            if(I18n.exists(path + ".tooltip.summary")) {
                if (Screen.hasShiftDown()) {
                    tooltipList.add(Lang.translateDirect("tooltip.holdForDescription", Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.DARK_GRAY));
                    tooltipList.add(Component.empty());
                    
                    //Yeah this is VERY UGLY, I know but I don't want to add interface and implement custom handling for it
                    boolean modifiedSummary = false;
                    String summary = "";
                    if (item == THRUSTER_BLOCK.asItem()) {
                        int thrusterStrength = Math.round(ThrusterBlockEntity.BASE_MAX_THRUST / 1000.0f * Config.THRUSTER_THRUST_MULTIPLIER.get());
                        summary = Component.translatable(path + ".tooltip.summary").getString().replace("{}", String.valueOf(thrusterStrength));
                        modifiedSummary = true;
                    }

                    if (item == INLINE_OPTICAL_SENSOR_BLOCK.asItem()) {
                        int raycastDistance = Config.INLINE_OPTICAL_SENSOR_MAX_DISTANCE.get();
                        summary = Component.translatable(path + ".tooltip.summary").getString().replace("{}", String.valueOf(raycastDistance));
                        modifiedSummary = true;
                    }

                    if (modifiedSummary) {
                        tooltipList.addAll(TooltipHelper.cutStringTextComponent(summary, Palette.STANDARD_CREATE));
                    } else {
                        tooltipList.addAll(TooltipHelper.cutStringTextComponent(Component.translatable(path + ".tooltip.summary").getString(), Palette.STANDARD_CREATE));
                    }

                    //Yeah this only supports up to 2 conditions
                    if(!Component.translatable(path + ".tooltip.condition1").getString().equals(path + ".tooltip.condition1")) {
                        tooltipList.add(Component.empty());
                        tooltipList.add(Component.translatable(path + ".tooltip.condition1").withStyle(ChatFormatting.GRAY));
                        tooltipList.addAll(TooltipHelper.cutStringTextComponent(Component.translatable(path + ".tooltip.behaviour1").getString(), Palette.STANDARD_CREATE.primary(), Palette.STANDARD_CREATE.highlight(), 1));
                        if(!Component.translatable(path + ".tooltip.condition2").getString().equals(path + ".tooltip.condition2")) {
                            tooltipList.add(Component.translatable(path + ".tooltip.condition2").withStyle(ChatFormatting.GRAY));
                            tooltipList.addAll(TooltipHelper.cutStringTextComponent(Component.translatable(path + ".tooltip.behaviour2").getString(), Palette.STANDARD_CREATE.primary(), Palette.STANDARD_CREATE.highlight(), 1));
                        }
                    }

                } else {
                    tooltipList.add(Lang.translateDirect("tooltip.holdForDescription", Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            tooltip.addAll(1, tooltipList);
        }
    }
}
