package com.deltasf.createpropulsion.utility;

import com.deltasf.createpropulsion.CreatePropulsion;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Bakery {
    public static final BakedModel[] BAKED_COMPASS_MODELS = new BakedModel[32];
    public static final ModelResourceLocation[] COMPASS_MODELS = new ModelResourceLocation[32];
    static {
        for (int i = 0; i < 32; ++i) {
            String modelPath = String.format("compass_%02d", i);
            COMPASS_MODELS[i] = ModelResourceLocation.vanilla(modelPath, "inventory");
        }
    }
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (int i = 0; i < 32; ++i) {
            event.register(COMPASS_MODELS[i]);
        }
    }

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        ModelManager modelManager = event.getModelManager();
        for (int i = 0; i < 32; i++) {
            BAKED_COMPASS_MODELS[i] = modelManager.getModel(COMPASS_MODELS[i]);
        }
    }
}
