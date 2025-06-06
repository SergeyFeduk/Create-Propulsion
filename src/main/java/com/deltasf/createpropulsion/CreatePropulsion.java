package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.optical_sensors.InlineOpticalSensorBlock;
import com.deltasf.createpropulsion.optical_sensors.InlineOpticalSensorBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlock;
import com.deltasf.createpropulsion.optical_sensors.OpticalSensorBlockEntity;
import com.deltasf.createpropulsion.optical_sensors.rendering.OpticalSensorRenderer;
import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.deltasf.createpropulsion.ponder.SFPonderPlugin;
import com.deltasf.createpropulsion.thruster.ThrusterBlock;
import com.deltasf.createpropulsion.thruster.ThrusterBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.network.chat.Component;
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
        .properties(p -> p.mapColor(MapColor.METAL))
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
        .properties(p -> p.mapColor(MapColor.METAL))
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
        
    public CreatePropulsion() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ParticleTypes.register(modBus);
        modBus.addListener(this::onClientSetup);
        //Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC, ID + "-server.toml");

        CreativeModTab.register(modBus);

        REGISTRATE.registerEventListeners(modBus);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
		PonderIndex.addPlugin(new SFPonderPlugin());
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
            }
        }
    }

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
                    tooltipList.add(CreateLang.translateDirect("tooltip.holdForDescription", Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.DARK_GRAY));
                    tooltipList.add(Component.empty());
                    
                    //Yeah this is VERY UGLY, I know but I don't want to add interface and implement custom handling for it
                    boolean modifiedSummary = false;
                    String summary = "";
                    if (item == THRUSTER_BLOCK.asItem()) {
                        float thrustMultiplier = (float)(double)Config.THRUSTER_THRUST_MULTIPLIER.get();
                        int thrusterStrength = Math.round(ThrusterBlockEntity.BASE_MAX_THRUST / 1000.0f * thrustMultiplier);
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
                    tooltipList.add(CreateLang.translateDirect("tooltip.holdForDescription", Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            tooltip.addAll(1, tooltipList);
        }
    }
}
