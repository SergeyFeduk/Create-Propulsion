package com.deltasf.createpropulsion.atmosphere;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.atmosphere.data.DimensionAtmosphere;
import com.deltasf.createpropulsion.atmosphere.data.VarianceNoiseProperties;
import com.deltasf.createpropulsion.utility.math.NoiseOctave;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

public class DimensionAtmosphereManager extends SimpleJsonResourceReloadListener {
    public record AtmosphereProperties(double pressureAtSeaLevel, double scaleHeight, double gravity, int seaLevel, boolean isAirless, VarianceNoiseProperties varianceNoise) {}
    private static final double epsilon = 1e-5;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "atmospheres";

    private static Map<ResourceKey<Level>, AtmosphereProperties> atmosphereMap = new HashMap<>();

    public static final VarianceNoiseProperties DEFAULT_NOISE = new VarianceNoiseProperties(0.1, -0.05, 0.2, List.of(new NoiseOctave(250, 0.03), new NoiseOctave(80, 0.015)));
    public static final int DEFAULT_SEA_LEVEL = 63;
    public static final AtmosphereProperties DEFAULT = new AtmosphereProperties(1.225, 200.0, 9.81, DEFAULT_SEA_LEVEL, false, DEFAULT_NOISE);

    public DimensionAtmosphereManager() {
        super(GSON, DIRECTORY);
    }

    public static AtmosphereProperties getProperties(ResourceKey<Level> dimKey) {
        return atmosphereMap.getOrDefault(dimKey, DEFAULT);
    }

    public static AtmosphereProperties getProperties(Level level) {
        return atmosphereMap.getOrDefault(level.dimension(), DEFAULT);
    }

    public static AtmosphereData getData(Level level) {
        return getData(level.dimension().location().toString());
    }

    public static AtmosphereData getData(String dimensionId) {
        final String PREFIX = "minecraft:dimension:";
        if (dimensionId.startsWith(PREFIX)) {
            dimensionId = dimensionId.substring(PREFIX.length());
        }

        ResourceLocation location = ResourceLocation.parse(dimensionId);
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
        
        AtmosphereProperties properties = atmosphereMap.get(key);
        boolean isFallback = false;

        if (properties == null) {
            properties = DEFAULT;
            isFallback = true;
        }

        if (properties.isAirless()) {
            return new AtmosphereData(0.0, 0.0, properties.gravity(), properties.seaLevel(), true, properties.varianceNoise(), isFallback);
        }

        double scaleHeight = properties.scaleHeight() > epsilon ? properties.scaleHeight() : DEFAULT.scaleHeight();

        return new AtmosphereData(
            properties.pressureAtSeaLevel(), 
            scaleHeight, 
            properties.gravity(),
            properties.seaLevel(),
            false,
            properties.varianceNoise(),
            isFallback
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
                ResourceLocation dimensionId = ResourceLocation.parse(file.getPath());
                ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
                VarianceNoiseProperties noise = definition.varianceNoise().orElse(DEFAULT_NOISE);

                boolean airless = definition.isAirless().orElse(false);
                if (airless) {
                    newMap.put(dimensionKey, new AtmosphereProperties(
                        0.0, 
                        DEFAULT.scaleHeight(),
                        definition.gravity().orElse(DEFAULT.gravity()),
                        definition.seaLevel().orElse(DEFAULT_SEA_LEVEL),
                        true,
                        noise
                    ));
                } else {
                    AtmosphereProperties properties = new AtmosphereProperties(
                        definition.pressureAtSeaLevel().orElse(DEFAULT.pressureAtSeaLevel()),
                        definition.scaleHeight().get(),
                        definition.gravity().orElse(DEFAULT.gravity()),
                        definition.seaLevel().orElse(DEFAULT_SEA_LEVEL),
                        false,
                        noise
                    );
                    newMap.put(dimensionKey, properties);
                }
            });
        }

        atmosphereMap = newMap;
        profiler.pop();
    }
}
