package com.deltasf.createpropulsion.atmosphere;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

public class DimensionAtmosphereManager extends SimpleJsonResourceReloadListener {
    public record AtmosphereProperties(double pressureAtSeaLevel, double scaleHeight, double gravity, boolean isAirless) {}
    private static final double epsilon = 1e-5;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "atmospheres";

    private static Map<ResourceKey<Level>, AtmosphereProperties> atmosphereMap = new HashMap<>();
    public static final AtmosphereProperties DEFAULT = new AtmosphereProperties(1.225, 200.0, 9.81, false);

    public DimensionAtmosphereManager() {
        super(GSON, DIRECTORY);
    }

    public static AtmosphereProperties getProperties(Level level) {
        return atmosphereMap.getOrDefault(level.dimension(), DEFAULT);
    }

    public static AtmosphereData getData(Level level) {
        AtmosphereProperties properties = getProperties(level);

        if (properties.isAirless()) {
            return new AtmosphereData(0.0, 0.0, properties.gravity(), AtmoshpereHelper.determineSeaLevel((ServerLevel)level), true);
        }

        double scaleHeight = properties.scaleHeight() > epsilon ? properties.scaleHeight() : DimensionAtmosphereManager.DEFAULT.scaleHeight();

        return new AtmosphereData(
            properties.pressureAtSeaLevel(), 
            scaleHeight, 
            properties.gravity(),
            AtmoshpereHelper.determineSeaLevel((ServerLevel)level),
            false
        );
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> pObject, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        profiler.push(CreatePropulsion.ID + ":loading_balloon_atmospheres");
        Map<ResourceKey<Level>, AtmosphereProperties> newMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation file = entry.getKey();
            JsonElement json = entry.getValue();

            DimensionAtmosphere.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(error -> LOGGER.error("[{}] Failed to parse atmosphere definition from {}: {}", CreatePropulsion.ID, file, error))
            .ifPresent(definition -> {
                if (definition.requiredMod().isPresent() && !ModList.get().isLoaded(definition.requiredMod().get())) {
                    return;
                }
                ResourceLocation dimensionId = new ResourceLocation(file.getPath());
                ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);

                boolean airless = definition.isAirless().orElse(false);
                if (airless) {
                    newMap.put(dimensionKey, new AtmosphereProperties(
                        0.0, 
                        DEFAULT.scaleHeight(), // Not used, but good to have a value
                        definition.gravity().orElse(DEFAULT.gravity()),
                        true
                    ));
                } else {
                    AtmosphereProperties properties = new AtmosphereProperties(
                        definition.pressureAtSeaLevel().orElse(DEFAULT.pressureAtSeaLevel()),
                        definition.scaleHeight().get(),
                        definition.gravity().orElse(DEFAULT.gravity()),
                        false
                    );
                    newMap.put(dimensionKey, properties);
                }
            });
        }

        atmosphereMap = newMap;
        profiler.pop();
    }
}
