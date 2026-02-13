package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.ponder.DeltaPonderPlugin;
import com.deltasf.createpropulsion.registries.PropulsionInstanceTypes;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class PropulsionClient {

    @OnlyIn(Dist.CLIENT)
    public static void openConfig() {
        Screen parent = Minecraft.getInstance().screen;
        ScreenOpener.open(new BaseConfigScreen(parent, CreatePropulsion.ID));
    }

    public static void clientInit(final FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new DeltaPonderPlugin());
        PropulsionInstanceTypes.register();
    }
}
