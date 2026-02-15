package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.ponder.DeltaPonderPlugin;
import com.deltasf.createpropulsion.registries.PropulsionInstanceTypes;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=CreatePropulsion.ID, bus= Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class PropulsionClient {

    @SubscribeEvent
    public static void onClientCommandsRegister(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> propulsionCommand = Commands.literal("propulsion");
        propulsionCommand
                .then(Commands.literal("config")
                        .executes((ctx) -> {
                            openConfig();
                            return 1;
                        }));
    }
    public static void openConfig() {
        Screen parent = Minecraft.getInstance().screen;
        ScreenOpener.open(new BaseConfigScreen(parent, CreatePropulsion.ID));
    }

    public static void clientInit(final FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new DeltaPonderPlugin());
        PropulsionInstanceTypes.register();
    }
}
