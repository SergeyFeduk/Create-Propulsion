package com.deltasf.createpropulsion.propeller.blades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BladeDataManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, BladeProperties> BLADES = new HashMap<>();

    public static final BladeDataManager INSTANCE = new BladeDataManager();

    public BladeDataManager() {
        super(GSON, "blades");
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> elements, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        BLADES.clear();
        elements.forEach((id, element) -> {
            BLADES.put(id, GSON.fromJson(element, BladeProperties.class));
        });
    }

    public static BladeProperties get(ResourceLocation id) {
        return BLADES.getOrDefault(id, BladeProperties.DEFAULT);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
