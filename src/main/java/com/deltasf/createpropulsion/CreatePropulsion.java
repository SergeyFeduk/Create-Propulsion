package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.deltasf.createpropulsion.thruster.ThrusterBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

import javax.annotation.Nonnull;

import com.simibubi.create.foundation.item.TooltipHelper.Palette;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab.Output;

@Mod(CreatePropulsion.ID)
public class CreatePropulsion {
    public static final String ID = "createpropulsion";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);
    public static CreateRegistrate registrate() { return REGISTRATE; }
    
    //Compats
    public static final boolean CDG_ACTIVE = ModList.get().isLoaded("createdieselgenerators");
    public static final boolean CBC_ACTIVE = ModList.get().isLoaded("createbigcannons");
    public static final boolean TFMG_ACTIVE = ModList.get().isLoaded("tfmg");
    public static final boolean SHIMMER_ACTIVE = ModList.get().isLoaded("shimmer");

    public CreatePropulsion() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        //Content
        ParticleTypes.register(modBus);
        PropulsionBlocks.register();
        PropulsionBlockEntities.register();
        PropulsionItems.register();
        PropulsionFluids.register();
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC, ID + "-server.toml");
        CreativeModTab.register(modBus);
        REGISTRATE.registerEventListeners(modBus);
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
            .icon(() -> PropulsionBlocks.THRUSTER_BLOCK.asStack())
            .displayItems(new RegistrateDisplayItemsGenerator())
            .build());

        public static void register(IEventBus modEventBus){
            REGISTER.register(modEventBus);
        }

        private static class RegistrateDisplayItemsGenerator implements DisplayItemsGenerator {
            public RegistrateDisplayItemsGenerator() {}

            @Override
            public void accept(@Nonnull ItemDisplayParameters parameters, @Nonnull Output output) {
                //From 0.1
                output.accept(PropulsionBlocks.INLINE_OPTICAL_SENSOR_BLOCK);
                output.accept(PropulsionBlocks.OPTICAL_SENSOR_BLOCK);
                output.accept(PropulsionBlocks.THRUSTER_BLOCK);
                //From 0.2
                output.accept(PropulsionBlocks.PHYSICS_ASSEMBLER_BLOCK);
                output.accept(PropulsionBlocks.LODESTONE_TRACKER_BLOCK);
                //From 0.2 (items)
                output.accept(PropulsionFluids.TURPENTINE.getBucket().get());
                output.accept(PropulsionItems.PINE_RESIN);
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
                    tooltipList.add(Lang.translateDirect("tooltip.holdForDescription", Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.DARK_GRAY));
                    tooltipList.add(Component.empty());
                    
                    //Yeah this is VERY UGLY, I know but I don't want to add interface and implement custom handling for it
                    boolean modifiedSummary = false;
                    String summary = "";
                    if (item == PropulsionBlocks.THRUSTER_BLOCK.asItem()) {
                        float thrustMultiplier = (float)(double)Config.THRUSTER_THRUST_MULTIPLIER.get();
                        int thrusterStrength = Math.round(ThrusterBlockEntity.BASE_MAX_THRUST / 1000.0f * thrustMultiplier);
                        summary = Component.translatable(path + ".tooltip.summary").getString().replace("{}", String.valueOf(thrusterStrength));
                        modifiedSummary = true;
                    }

                    if (item == PropulsionBlocks.INLINE_OPTICAL_SENSOR_BLOCK.asItem()) {
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
