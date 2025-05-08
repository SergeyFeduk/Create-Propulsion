package com.deltasf.createpropulsion;

import javax.annotation.Nonnull;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.CreativeModeTab.Output;

@EventBusSubscriber(bus = Bus.MOD)
public class PropulsionCreativeTab {
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
